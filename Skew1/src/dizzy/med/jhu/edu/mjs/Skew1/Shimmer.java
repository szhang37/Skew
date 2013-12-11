package dizzy.med.jhu.edu.mjs.Skew1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ArrayBlockingQueue;

import android.os.SystemClock;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

// *******************************************************************************
// *******************************************************************************
// *******************************************************************************
//
// Shimmer class. Each shimmer gets its own thread.
//
public class Shimmer {
//	public static final byte ShimmerSamplePeriod_ms = 2; // in mSec
//	public static final byte ShimmerSamplePeriod_ms = 20; // in mSec
	
	// Set in constructor
	public final byte ShimmerSamplePeriod_ms; // in mSec
	public final int NChannels;
	
	public static final int BYTES_PER_SAMPLE = 16;
    private static final String TAG = "EMGGraph-Shimmer";
	private static BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket btSocket;
	private OutputStream outStream;
	private InputStream inStream;
	private boolean isConnected = false;
    
	// Semaphore with 1 token (aka "permit").
	public Semaphore semaphore = new Semaphore(1);
	
	//private InQueue inQueue = new InQueue;

	// Object to lock on to allow only one Shimmer at a time to connect.
	public static Object ConnectLock = new Object();
	
	// Booleans used to control the thread's main run() loop.
	public static boolean
			ThreadKeepGoing = false,
			StayConnected = false,
			KeepStreaming = false;  // ALL Shimmer objects read this value.
	
	public enum ThreadState {
		NULL_STATE,
		BT_DISCONNECTED, BT_CONNECT, BT_CONNECTED,
		START_STREAMING, IS_STREAMING, STOP_STREAMING, NOT_STREAMING,
		BT_DISCONNECT, RESTART_STREAMING,
	};

	public ThreadState state = ThreadState.BT_DISCONNECTED; 
	
	private Random rand = new Random();
	
	// Well known SPP UUID.
	private final UUID MY_UUID = 
			UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	private Thread thread=null;
	public String MACAddress;
	private static Shimmer[] shimmers;

	// We first read in the raw packet bytes into PacketBytes[], then
	// they must be shuffled around to become proper integers in Packet[].
	private int[] PacketBytes = new int[BYTES_PER_SAMPLE];
	//  /*private*/public int[] Packet = new int[8];

	// This is the queue of packets that the main thread will read from.
	// The queue points into the array that follows, so we don't have to keep doing "new"
	// for each sample. It seems a bit round-about, but works well.
	private static final int QueueSize = 1000;
	public ArrayBlockingQueue<int[]> PacketQueue = new ArrayBlockingQueue<int[]>(QueueSize);
	private int[][] PacketQueueStore = new int[QueueSize][8];
	private int Qidx = 0;  // PacketQueueStore[] index
	
	String ShortName() {return MACAddress.substring(MACAddress.length() -5);}
	
	// Debug printing - adds the Shimmer MAC address to the debug string.
	void dprint(String format, Object ... args) {
		// Prepend our Shimmer address to the debug strings.
		format = String.format("SHIMMER %s: %s", ShortName(), format);
		Log.d(TAG, String.format(format, args));
	}
	
	// Static version of above, for static methods below.
	static void dprints(String format, Object ... args) {
		format = String.format("<static> %s", format);
		Log.d(TAG, String.format(format, args));
	}

	Shimmer(String address, int SampPeriod, int NChan) {
		MACAddress = address;
		ShimmerSamplePeriod_ms = (byte)SampPeriod;
		NChannels = NChan;
	}

	
	// ===============================================================================
	// ===============================================================================
	// Static interface methods, work on array of Shimmer[] objects.
	//
	
	// For simplicity, these methods mirror the application methods that are called
	// when the application changes states, and these should be called from those
	// methods.
	//
	// These are static methods that work on the array of Shimmer[]s.

	// After OnStart() completes, all threads should be started, and all preliminary
	// BlueTooth socket activities should be complete, except for connect().
	public static void onStart(Shimmer[] shimmers)
	{
		// Get handle to BlueTooth adapter, but do not connect() to
		// Shimmers yet.
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Log.d(TAG, "Bluetooth is not available."); 
			return;
		}

		dprints("Got BlueTooth Handle");

		if (!mBluetoothAdapter.isEnabled()) {
			Log.d(TAG, "Please enable your BT and re-run this program.");
			return;
		}
		dprints("is Enabled");
		// Save "local" pointer to shimmers array.
		Shimmer.shimmers = shimmers;
		
		// Start all Shimmer threads.
		ThreadKeepGoing = true;
		for(Shimmer s: shimmers) {
			s.StartThread();
		}
		dprints("Starting over");
	}


	// !! All of the functions below here must work on the threads, since that is where
	// the action will occur. They cannot just "do" something. They must
	// tell the thread to "do something", via the boolean control variables.
	
	// OnResume() should cause all connections to be checked, and start or re-start any
	// that are not connect()ed.
	public static void onResume()
	{
		//mBluetoothAdapter.cancelDiscovery();
		StayConnected = true;
		KeepStreaming = true;
		//ThreadKeepGoing = true;
		for(Shimmer s: shimmers) {
			//synchronized(s.thread) {
			//synchronized(s) {
			//	dprints("Sending notify() to %s", s.ShortName());
			//	s.notify();
			//}
			s.state = ThreadState.BT_CONNECT;
			dprints("releas()ing semaphore for %s", s.ShortName());
			s.semaphore.release();
			//s.thread.start();
			//if(s.thread.isAlive()) dprints("%s","thread interrupted!");
		}
	}

	// OnPause() should send the Shimmer StopStreaming command to all Shimmers.
	public static void onPause()
	{
		dprints("onPause()");
		return;/*
		for(Shimmer s: shimmers) {
			s.state = ThreadState.STOP_STREAMING;
			s.semaphore.release();
		}
		*/ 	
	}
	

	// OnStop() should disconnect() all BlueTooth sockets and release resources.
	// All threads should be "join()ed" to make sure they end cleanly.
	public static void onStop()
	{
		for(Shimmer s: shimmers) {
			s.StopThread();
			while(true) try{s.thread.join(); break;} catch (InterruptedException e){}
		}
	}

	public static void onStopStreaming()
	{
		for(Shimmer s: shimmers) {
			s.state = ThreadState.STOP_STREAMING;
			s.semaphore.release();
		}
	}

	public static void onStartStreaming()
	{
		// The "nice" way.
		//for(Shimmer s: shimmers) {
		//	s.state = ThreadState.RESTART_STREAMING;
		//	s.semaphore.release();
		//}
		
		// The "right" way. Call all from w/in this thread, so it happens "now".
		for(Shimmer s: shimmers)
			s.PacketQueue.clear();

		// Do all Shimmers "at once".
		for(Shimmer s: shimmers) {
			// Send the StartStreaming command.
			try {
				s.outStream.write(new byte[] {0x07});
			} catch (IOException e) {
				Log.e(TAG, "Start Shimmer failed", e);
			}
		}

		for(Shimmer s: shimmers) {
			s.state = ThreadState.IS_STREAMING;
			s.semaphore.release();
		}
	}
	
	
	// ===============================================================================
	// ===============================================================================
	// Non-static, "per-instance" methods, work in separate threads on single
	// Shimmer object.
	
	public void StartThread()
	{
	    thread = new Thread(ShimmerThreadFunc);
	    thread.start();
	}

	public void StopThread()
	{
		ThreadKeepGoing = false;
		StayConnected = false;
		KeepStreaming = false;
	}

	// Wait for an interrupt, or until the timer expires.
	// Used in the main thread loop to wait for changes in our boolean state variables.
	void WaitInterrupt()
	{
		try {Thread.sleep(200);}
		catch (InterruptedException e) {
			return;
		}
	}

	// A better way to "wait". We wait() for a notify(). To do this, we must be in 
	// a "synchronized" section.
	void WaitNotify()
	{
		while(true) {
			try {
				semaphore.acquire();
				break;
/*
				synchronized(this) {
					wait();
					break;
				}
*/
			}
			catch (InterruptedException e) {
				// If we get interrupted, we don't do anything. Keep waiting...
			}
			/*
			catch(IllegalMonitorStateException e) {
				dprint("IllegalMonitorStateException. Should not get this.");
				return;
			}
			*/
		}
	}
	
	// This is the thread main loop, a "runnable" function passed to the 
	// thread object in StartThread(). It starts the Shimmer and
	// continuously reads in packets while ThreadKeepGoing is TRUE.
	private Runnable ShimmerThreadFunc = new Runnable() {
    	public void run() {
    		dprint("%s","Running the shimmer thread.");
    		if(!SetupBluetooth()) {
    			dprint("Returning from run() early.");
				ThreadKeepGoing = false;
    			return;
    		}
    		
    		ThreadState laststate = ThreadState.NULL_STATE;
    		
    		while(ThreadKeepGoing) {
    			if(state != laststate)
    				dprint("state: %s", state);
    			laststate = state;
    			switch(state) {
    			
   				case BT_CONNECT:
   					if(!BluetoothConnect())
   						return;
   					//state = ThreadState.BT_CONNECTED;
   					state = ThreadState.START_STREAMING;
   					break;

   				case START_STREAMING:
   		    		if(!InitializeShimmer())
   		    			return;
   		    		state = ThreadState.IS_STREAMING;
   					break;

   				case RESTART_STREAMING:
   					ShimmerStartStreaming();
   					state = ThreadState.IS_STREAMING;
   					break;

   				case IS_STREAMING:
   					// Read and queue a packet.
   					ReadPacket();
   					break;

   				case STOP_STREAMING:
   					StopShimmer();
   					state = ThreadState.NOT_STREAMING;
   					
   				case BT_DISCONNECTED:
   				case BT_CONNECTED:
   				case NOT_STREAMING:
   					// For these states, we are idly waiting for the next command.
   					WaitNotify();
   					break;
    		}
    		}
    		CloseShimmerBluetooth();
    	}
	};

	// Do as much setup as possible, just short of connect()ing.
	public boolean SetupBluetooth()
	{
		dprint("In SetupBluetooth(%s)", MACAddress);
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MACAddress);

		// Make sure it is closed out first.
		CloseShimmerBluetooth();

		try {
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			
			dprint("got btSocket");

			outStream = btSocket.getOutputStream();
			dprint("got outStream");

			inStream = btSocket.getInputStream();
			dprint("got inStream");
			
			// Flush out the input stream.
			//inStream.skip(inStream.available());
			//dprint("inStream.skip()");
		} catch (IOException e) {
			//Log.e(TAG, String.format("OpenShimmer(%s) failed '%s'", MACAddress, e.getMessage()), e);
			dprint("OpenShimmer failed '%s'", e.getMessage());
			CloseShimmerBluetooth();
			return false;
		}
		dprint("exiting OpenShimmerBluetooth()");
		return true;
	}

	public boolean BluetoothConnect()
	{
		int loopcnt=0;

		for(loopcnt=0; loopcnt<4; ++loopcnt) {
			dprint("connect() try %d", loopcnt);
		try {
			// This one does the real "hardware" work of actually starting up the radio link.
			synchronized(ConnectLock) {
				btSocket.connect();
			}
			dprint("connect()'ed");
			isConnected = true;
			return true;
		} catch(IOException e) {
			dprint("Connect to Shimmer failed");
			SystemClock.sleep(200+rand.nextInt(10)*200);
		}
		}
		return false;
	}
	
	public void CloseShimmerBluetooth()
	{	
		try {
			if(inStream != null)
				inStream.close();
			if(outStream != null)
				outStream.close();
			if(btSocket != null)
				btSocket.close();

			inStream = null;
			outStream = null;
			btSocket = null;
		} catch(IOException e) {
		}
	}

	// Send configuration commands to shimmers to make sure they are
	// all set up in the same way.	
	private boolean InitializeShimmer() {
		try {
		
			// Clear out the packet queue.
			PacketQueue.clear();
			
			// Set sample rate.
			outStream.write(new byte[] {0x05, ShimmerSamplePeriod_ms});
			SystemClock.sleep(150);
			
			// Select which sensors. Accel and Gyro.
			// Maybe should add battery at some point, but not today.
			if(NChannels == 1)
				// Just EMG.
				outStream.write(new byte[] {0x08, (byte)0x08, 0x00});
			else if(NChannels == 3)
				// Just accel.
				outStream.write(new byte[] {0x08, (byte)0x80, 0x00});
			else if(NChannels == 4)
				// Accel + EMG
				outStream.write(new byte[] {0x08, (byte)0x88, 0x00});
			else if(NChannels == 6)
				// Accel + gyro
				outStream.write(new byte[] {0x08, (byte)0xC0, 0x00});
			SystemClock.sleep(7000);

			// Set accel range to +/-6G
			outStream.write(new byte[] {0x09, 0x03});
			SystemClock.sleep(150);

			// Send the StartStreaming command.
			outStream.write(new byte[] {0x07});
			
		} catch (IOException e) {
			Log.e(TAG, "Start Shimmer failed", e);
			return false;
		}
		
		//ShimmerRunning = true;
		return true;
	}

	boolean ShimmerStartStreaming()
	{
		// Clear out the packet queue.
		PacketQueue.clear();
		// Send the StartStreaming command.
		try {
			outStream.write(new byte[] {0x07});
		} catch (IOException e) {
			Log.e(TAG, "Start Shimmer failed", e);
			return false;
		}
		return true;
	}
	
	boolean StopShimmer() {
		//ShimmerRunning = false;
		dprint("Stopping streaming...");
		try {
			// Send the StopStreaming command.
			outStream.write(new byte[] {0x20});
			dprint("Streaming stopped.");
		} catch (IOException e) {
			Log.e(TAG, "Stop Shimmer failed", e);
			return false;
		}
		return true;
	}
	
	
	// For debugging queue.
	//private int PacketCounter = 0;

	// This will read bytes from the Shimmer Bluetooth channel into our PacketBytes[]
	// array. Once we have a full packet, we transfer the values to Packet[].
	public boolean ReadPacket() {
		   int InByte=0;
		   int bytenum = -1;
		   
		   if(inStream == null)
			   return false;
	
		   while(true) {
				try {
					InByte = inStream.read();
				} catch(IOException e){return false;}
			   
			   // Wait for '0' start byte.
			   if(bytenum == -1) {
				   if(InByte != 0)
					   continue;
				   ++bytenum;
				   continue;
			   }
	
			   PacketBytes[bytenum] = InByte;
//			   if(++bytenum >= 14)
			   if(++bytenum >= 2+NChannels*2)
				   break;
		   }
	
	//	   for(int i=0; i < 6; ++i)
	//		   Packet[UnitNum*6 + i] = PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048;
	
			// We correct the Shimmer signs and channel order right here. We want a
			// consistent "RightHand Rule" system, so that the signs of +X, +Y, and
			// +Z accel and gyro are correct. With the Shimmer logo facing you, +X
			// is up, +Y is left, and +Z is towards you.
	
		   	// Signs on X and Y accel are incorrect. 
		   	// X and Y gyros are swapped, so we swap them back.
		    int i;
		    // Time stamp.
		    PacketQueueStore[Qidx][0] = (PacketBytes[0] + PacketBytes[1]*256);
    
		    // Analog data. Accel.
            i=0;PacketQueueStore[Qidx][1] = -(PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048);
            
            if(NChannels >= 3) {
            	i=1;PacketQueueStore[Qidx][2] = -(PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048);
            	i=2;PacketQueueStore[Qidx][3] =  (PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048);
            }

			if(NChannels == 4) {
				// Just EMG
				i=3;PacketQueueStore[Qidx][4] = -(PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048);
			}

			if(NChannels == 6) {
				// We swap the X and Y gyros.
				i=4;PacketQueueStore[Qidx][4] = -(PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048);
				i=3;PacketQueueStore[Qidx][5] = -(PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048);
				i=5;PacketQueueStore[Qidx][6] = -(PacketBytes[2+i*2] + PacketBytes[2+i*2+1]*256 - 2048);
			}
			
			// For debugging the queue.
			//PacketQueueStore[Qidx][1] = PacketCounter++;
			//PacketQueueStore[Qidx][2] = PacketQueue.size();
			//PacketQueueStore[Qidx][3] = Qidx;
			
			try{PacketQueue.put(PacketQueueStore[Qidx]);} catch(InterruptedException e){}
			++Qidx;
			if(Qidx >= QueueSize)
				Qidx = 0;
			
			return true;
	}
	
	
	public boolean isConnected(){
		//Log.d(TAG, String.format("%s", state.name()));
		if(state!=ThreadState.IS_STREAMING) return false;
		else return true;
	}
}	
