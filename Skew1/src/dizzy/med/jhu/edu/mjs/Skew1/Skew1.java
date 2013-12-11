package dizzy.med.jhu.edu.mjs.Skew1;

// Changes March 26, 2013, ms
// Add date/time to data file stream to get time-to-complete.
// Only show data at bottom if file storage is off.
// Set alpha values for lines to reduce intensity and allow overlap.

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
@SuppressWarnings("deprecation")
public class Skew1 extends Activity {
	/** Called when the activity is first created. */
	//Shimmer initialization
	private static final String TAG = "EMG";
	private static Shimmer[] shimmers =  {
		//new Shimmer("00:06:66:46:BD:C4", 10, 6)
		new Shimmer("00:06:66:A0:38:B5", 10, 6),
		new Shimmer("00:06:66:A0:39:18", 10, 6),
		new Shimmer("00:06:66:A0:39:0B", 10, 6),
		new Shimmer("00:06:66:A0:39:1F", 10, 6),
	};
	private String shimmerFileName = null;
	private OutputStream shimmerStream = null;
	private boolean isShimmerEnabled =false;
	
	//Skew initialization
	private DrawLine mLine1;
	private TextView mText;
	private TextView mTrialText;
	private RadioGroup radioGroupVT;
	private RadioGroup radioGroupRB;
	private Button mButtonTopR;
	private Button mButtonBotR;
	private Button mButtonBotL;
	private Button mButtonRadV;
	private Button mButtonRadT;
	private Button mButtonRadR;
	private Button mButtonRadB;
	private Button mButtonOpen;
	private Button mButtonClose;
	private ToggleButton sToggleButton;
	private CheckBox sConnCheck1;
	private CheckBox sConnCheck2;
	private CheckBox sConnCheck3;
	private CheckBox sConnCheck4;
	private int mode; // 1=vert, 2=tor
	private Random random = new Random();
	private String dataFileName = null;
	private OutputStream dataStream = null;
	private SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS, z"); // the date format for file record
	DrawLine drawLine1;
	DrawLine drawLine2;
	private boolean firstTrial = true;
	private Timer checkerTimer;
	private Timer collectShimmerTimer;
	private boolean toggleOn = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//start the shimmers
		setContentView(R.layout.main);

		Intent intent = new Intent(this, SensorService.class);
		startService(intent);

		drawLine1 = new DrawLine(this, null);
		drawLine1.setBackgroundColor(Color.BLACK);
		// accel = new SensorActivity();
		 radioGroupVT = (RadioGroup) findViewById(R.id.radio_VT);
		radioGroupRB = (RadioGroup) findViewById(R.id.radio_RB);
		// get handles to Views defined in our layout file
		mLine1 = (DrawLine) findViewById(R.id.draw_line);
		// mLine2 = (DrawLine)findViewById(R.id.line2);
		mText = (TextView) findViewById(R.id.text);
		mTrialText = (TextView) findViewById(R.id.texttrial);
		mButtonBotR = (Button) findViewById(R.id.button2);
		mButtonBotL = (Button) findViewById(R.id.button1);
		mButtonTopR = (Button) findViewById(R.id.button4);
		mButtonRadV = (Button) findViewById(R.id.radioVertical);
		mButtonRadT = (Button) findViewById(R.id.radioTorsional);
		mButtonRadR = (Button) findViewById(R.id.radioRed);
		mButtonRadB = (Button) findViewById(R.id.radioBlue);
		mButtonOpen = (Button) findViewById(R.id.button_newfile);
		mButtonClose = (Button) findViewById(R.id.button_closefile);
//		radioGroupON = (RadioGroup) findViewById(R.id.radio_glass);
//		mButtonRadNew = (Button) findViewById(R.id.radioNewGlass);
//		mButtonRadOld = (Button) findViewById(R.id.radioOldGlass);
		sToggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
		sConnCheck1 = (CheckBox) findViewById(R.id.shimmer1);
		sConnCheck2 = (CheckBox) findViewById(R.id.shimmer2);
		sConnCheck3 = (CheckBox) findViewById(R.id.shimmer3);
		sConnCheck4 = (CheckBox) findViewById(R.id.shimmer4);
		sConnCheck1.setClickable(false);
		sConnCheck2.setClickable(false);
		sConnCheck3.setClickable(false);
		sConnCheck4.setClickable(false);
		sConnCheck1.setVisibility(View.INVISIBLE);
		sConnCheck2.setVisibility(View.INVISIBLE);
		sConnCheck3.setVisibility(View.INVISIBLE);
		sConnCheck4.setVisibility(View.INVISIBLE);
		sConnCheck1.setText(String.format("%s", "38B5"));
		sConnCheck2.setText(String.format("%s", "3918"));
		sConnCheck3.setText(String.format("%s", "390B"));
		sConnCheck4.setText(String.format("%s", "391F"));
		// radioGroupVT.check(R.id.radioVertical);
		radioGroupRB.check(R.id.radioRed);
//		radioGroupON.check(R.id.radioNewGlass);
		sToggleButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!toggleOn){
					sConnCheck1.setVisibility(View.VISIBLE);
					sConnCheck2.setVisibility(View.VISIBLE);
					sConnCheck3.setVisibility(View.VISIBLE);
					sConnCheck4.setVisibility(View.VISIBLE);
					isShimmerEnabled = true;
					dprint("In onStart()...");
					Shimmer.onStart(shimmers);
					dprint("done onStart().");
					Shimmer.onPause();
					Shimmer.onResume();
			        checkerTimer = new Timer();
			        checkerTimer.scheduleAtFixedRate(
			        		new TimerTask() {public void run() {checkTimeMethod();}},
			        		0, 1000);
			        collectShimmerTimer = new Timer();
			        collectShimmerTimer.scheduleAtFixedRate(
			        		new TimerTask() {public void run() {collectShimmerData();}},
			        		0, 100);   
			        toggleOn = true;
			        isShimmerEnabled = true;

				}
				else{
					sConnCheck1.setVisibility(View.INVISIBLE);
					sConnCheck2.setVisibility(View.INVISIBLE);
					sConnCheck3.setVisibility(View.INVISIBLE);
					sConnCheck4.setVisibility(View.INVISIBLE);
					
					isShimmerEnabled = false;
					Shimmer.onPause();
					checkerTimer.cancel();
					collectShimmerTimer.cancel();
					toggleOn = false;
				}
				//sToggleButton.setClickable(false);
				}
			
		});
		// up or cw
		mButtonBotR.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mode == 1)
					Global.ypos = Global.ypos - 1;
				if (mode == 2)
					Global.yang = Global.yang + 1;
				modifyInfoText();
				mLine1.postInvalidate();
				trackMiddleData();
			}
		});

		// down or ccw
		mButtonBotL.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mode == 1)
					Global.ypos = Global.ypos + 1;
				if (mode == 2)
					Global.yang = Global.yang - 1;
				modifyInfoText();
				mLine1.postInvalidate();
				trackMiddleData();
			}
		});

		// new trial
		mButtonTopR.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
            if(!firstTrial){
            	
            	mTrialText.setText(String.format("%s", "Trial     #"+Global.trialIndex));
            	
            	
				if (dataStream != null) {
					/* write current color mode into file */
					String color;
					if (Global.color == Color.RED)
						color = "RED";
					else
						color = "BLUE";
					PrintStream ps = new PrintStream(dataStream);
					Date date = new Date();
					ps.println(String.format(
							"end    ypos %d  yang %d  %s  x %f  y %f  z %f %s",
							Global.ypos-500, Global.yang, sdf.format(date),
							Global.yaw, Global.pitch, Global.roll, color));
				}
				else{
					//Global.trialIndex =0;
				}
               }
            else{
            	Global.trialIndex =0;
            	mTrialText.setText(String.format("%s", "Trial     #"+Global.trialIndex));
            }
            firstTrial = false;
				/* generate a random number [4,15] or [-15,-4] */
				int randomNum = random.nextInt(12) + 4;
				boolean randomC = random.nextBoolean();
				if (randomC)
					randomNum = -randomNum;
				if (mode == 1)
					/* if mode 1, then ypos values [504,515] or [485, 496] */
					Global.ypos = 500 + randomNum;
				if (mode == 2)
					/* if mode 2, then yang values [4,15] or [-15, -4] */
					Global.yang = randomNum;
				if (dataStream != null) {
					/* write current color mode into file */
					String color;
					if (Global.color == Color.RED)
						color = "RED";
					else
						color = "BLUE";
					PrintStream ps = new PrintStream(dataStream);
					Date date = new Date();
					ps.println(String.format(
							"start  ypos %d  yang %d  %s  x %f  y %f  z %f %s",
							Global.ypos-500, Global.yang, sdf.format(date),
							Global.yaw, Global.pitch, Global.roll, color));
				}
				modifyInfoText();
				
				Global.trialIndex ++;
				
				mLine1.postInvalidate();
			}
		});

		// set Red mode
		mButtonRadR.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Global.ypos = 500;
				Global.yang = 0;
				Global.color = Color.RED;
				mLine1.postInvalidate();
			}
		});
		// set Blue mode
		mButtonRadB.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Global.ypos = 500;
				Global.yang = 0;
				Global.color = Color.BLUE;
				mLine1.postInvalidate();
			}
		});
		
//		// set New Glass mode
//				mButtonRadNew.setOnClickListener(new View.OnClickListener() {
//					public void onClick(View v) {
//						Global.red = getResources().getColor(R.color.newred);
//						mLine1.postInvalidate();
//					}
//				});
//				// set Old Glass mode
//				mButtonRadOld.setOnClickListener(new View.OnClickListener() {
//					public void onClick(View v) {
//						Global.red = getResources().getColor(R.color.red);
//						mLine1.postInvalidate();
//					}
//				});

		// set vertical mode
		mButtonRadV.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mode = 1;
				Global.ypos = 500;
				Global.yang = 0;
				//modifyInfoText();
				mLine1.postInvalidate();
			}
		});

		// set torsional mode
		mButtonRadT.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mode = 2;
				Global.ypos = 500;
				Global.yang = 0;
				mLine1.postInvalidate();
			}
		});

		// open new file
		mButtonOpen.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				if (radioGroupVT.getCheckedRadioButtonId()!=-1 && dataStream == null) {
			    sToggleButton.setClickable(false);
				NewDataFile();
				mText.setText(String.format(dataFileName));
				mLine1.postInvalidate();
				firstTrial = true; 
				mButtonTopR.performClick();
				}
			}
		});

		// close file
		mButtonClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sToggleButton.setClickable(true);
				CloseDataFile();
				mText.setText(String.format("The File is closed"));
				mLine1.postInvalidate();
			}
		});

		// public void onButtonNewFile(View v) NewDataFile();

		// public void onButtonCloseFile(View v) CloseDataFile();
	}
	@Override
	protected void onStart(){
		super.onStart();
		//updateShimmerConnInfo();
	}
	private void checkTimeMethod(){
		runOnUiThread(new Runnable() {
		    public void run() {
		    	updateShimmerConnInfo();
		    }
		});
	}

	private void NewDataFile() {

		
		// First close old file, if it is open.
		if (dataStream != null)
			CloseDataFile();
		// mValueTV.setText("<No File>");

		Date date = new Date();
		String TypeString;
		if (mode == 1)
			TypeString = "V";
		else
			TypeString = "T";
		dataFileName = String.format("%4d_%02d%02d_%02d%02d%02d_skew_%s.txt",
				date.getYear() + 1900, date.getMonth() + 1, date.getDate(),
				date.getHours(), date.getMinutes(), date.getSeconds(),
				TypeString);

		// Can leave this in if you need to create a new directory. Otherwise,
		// the NASA directory
		// should be on both Androids.
		//
		// File newdir = new File(Environment.getExternalStorageDirectory()
		// + File.separator + "NASA");
		// newdir.mkdir();
		File sdir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "NASA"+ File.separator);
		sdir.mkdirs();
		File file = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "NASA" + File.separator + dataFileName);

		// Log.w(TAG, "Trying to write out to file.");

		try {
			
			file.createNewFile();
			dataStream = new FileOutputStream(file);
			if(isShimmerEnabled) NewShimmerDataFile(); 
		} catch (FileNotFoundException e) {
			// Log.e(TAG, "File Not Found!!!", e);
			return;
		} catch (IOException e) {
			// Log.e(TAG, "IO Exception writing to file.", e);
			return;
		}
    
		// mValueTV.setText(dataFileName);
	}

	private void CloseDataFile() {
		dataFileName = null;
		Global.trialIndex =0;
		if (dataStream != null) {
			String color;
			if (Global.color == Color.RED)
				color = "RED";
			else
				color = "BLUE";
			PrintStream ps = new PrintStream(dataStream);
			Date date = new Date();
			ps.println(String.format(
					"end    ypos %d  yang %d  %s  x %f  y %f  z %f %s",
					Global.ypos-500, Global.yang, sdf.format(date), Global.yaw,
					Global.pitch, Global.roll, color));
		}
		if (dataStream == null)
			return;
		try {
			dataStream.close();
			dataStream = null;
			
		} catch (IOException e) {
		}

		// mValueTV.setText("<No File>");

		// Trying to get it to flush the file out so it shows up immediately on
		// the PC.
		// THIS WORKS! Files are updated immediately in Windows.
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + Environment.getExternalStorageDirectory()
						+ "/NASA")));
		if(isShimmerEnabled) CloseShimmerDataFile(); 
	}

	/* to keep track of the middle data every time button pushed */
	private void trackMiddleData() {
		if (dataStream != null) {
			String color;
			if (Global.color == Color.RED)
				color = "RED";
			else
				color = "BLUE";
			PrintStream ps = new PrintStream(dataStream);
			Date date = new Date();
			ps.println(String.format(
					"mid    ypos %d  yang %d  %s  x %f  y %f  z %f %s",
					Global.ypos-500, Global.yang, sdf.format(date), Global.yaw,
					Global.pitch, Global.roll, color));
		}
	}

	@Override
	protected void onDestroy() {
		Intent intent = new Intent(this, SensorService.class);
		stopService(intent);
		if(isShimmerEnabled) checkerTimer.cancel();
		super.onDestroy(); // Theactivity is about to be destroyed.
	}
	@Override
	public void onResume() {
		if(isShimmerEnabled){
		Shimmer.onResume();
		updateShimmerConnInfo();
		dprint("In onResume(), before super.onResume()...");
		}
		super.onResume();
		dprint("In onResume()...");

		}
	@Override
	public void onPause() {
		if(isShimmerEnabled) Shimmer.onPause();
		//checkerTimer.cancel();		
		dprint("onPause() completed.");
		dprint("onPause() entered...");
		super.onPause();

	}

	@Override
	protected void onStop() {
		if(isShimmerEnabled) Shimmer.onStop();
		super.onStop();

	}
	
	private void modifyInfoText(){
		if (dataFileName == null) {
			mText.setText(String.format("Test Mode: pos offset %d angle offset %d",
					 Global.ypos-500, Global.yang));
		} else {
			String state = "Writable";
			if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())){state = "not Writable";};
			mText.setText(String.format("%s",dataFileName));
		}
	}
	
	// Debug printing - adds the Shimmer MAC address to the debug string.
	private void dprint(String format, Object ... args) {
		// Prepend our Shimmer address to the debug strings.
		format = String.format("MAIN: %s", format);
		Log.d(TAG, String.format(format, args));
	}
	
	private void updateShimmerConnInfo(){
		sConnCheck1.setChecked(shimmers[0].isConnected());
		sConnCheck2.setChecked(shimmers[1].isConnected());
		sConnCheck3.setChecked(shimmers[2].isConnected());
		sConnCheck4.setChecked(shimmers[3].isConnected());
	}
	
    // Dump some of our configuration strings into the header.
    private void WriteShimmerDataFileHeader() {
    	if(shimmerStream == null)
    		return;
    	
    	PrintStream ps = new PrintStream(shimmerStream);
		ps.println(String.format("BYTES_PER_SAMPLE %d", Shimmer.BYTES_PER_SAMPLE));
		ps.println("Shimmers: MAC, SamplePeriod, NChannels");
		int shimnum = 1;
		for(Shimmer s: shimmers) {
			ps.println(String.format("Shimmer%d: %s, %d, %d", shimnum, s.MACAddress, s.ShimmerSamplePeriod_ms, s.NChannels));
			++shimnum;
		}

		// Terminate header string with zero byte.
		try{shimmerStream.write(0);}
		catch(IOException e) {}
    }
    
	private void NewShimmerDataFile() {
		
		// First close old file, if it is open.
		if(shimmerStream != null) {
			CloseShimmerDataFile();
		}

        Date date = new Date();
        shimmerFileName = String.format("%4d_%02d%02d_%02d%02d%02d_EMGdata",
				date.getYear()+1900, date.getMonth()+1, date.getDate(),
				date.getHours(), date.getMinutes(), date.getSeconds());

		File newdir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "NASA"+File.separator+"Shimmer");
				newdir.mkdir();

		File file = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "NASA"
				+ File.separator +"Shimmer"+File.separator+ shimmerFileName+".txt");

		Log.w(TAG, "Trying to write out to file.");

		try {
	    	file.createNewFile();
	    	shimmerStream = new FileOutputStream(file);
			//PrintStream ps = new PrintStream(fo);
    		//ps.println("TEST string to file?? Oh… Hello World!");
			//fo.close();
		}
	    catch(FileNotFoundException e) {
	    	Log.e(TAG, "File Not Found!!!", e);
	    	return;}
	    catch(IOException e) {
	    	Log.e(TAG, "IO Exception writing to file.", e);
	    	return;}
		WriteShimmerDataFileHeader();
	}

	private void CloseShimmerDataFile()
	{
		if(shimmerStream == null)
			return;
		
		try {shimmerStream.close();}
		catch(IOException e) {}
		shimmerStream = null;
		shimmerFileName = null;
		
		// Trying to get it to flush the file out so it shows up immediately on the PC.
		// THIS WORKS! Files are updated immediately in Windows.
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://"
                        + Environment.getExternalStorageDirectory()
                        + "/NASA/Shimmer")));
	}
	
    // Reformat the Packet[] integers into swapped bytes for writing out to the data file.
    byte[] bytes = new byte[Shimmer.BYTES_PER_SAMPLE];
    String GetPacketBytes(int[] Packet, int UnitNum)
    {
    	for(int b=0; b<14; b+=2) {
    		bytes[b] = (byte)Packet[b/2];
    		bytes[b+1] = (byte)(Packet[b/2] >> 8);
    	}
    	// Save Shimmer unit number in last slot.
    	bytes[15] = (byte)UnitNum;
    	bytes[14] = 0;
    	//Log.w(TAG, "Byte data:"+System.currentTimeMillis());
    	String info = "";
    	int b[] =new int[16];
    	for(int i=0;i<16;i++) b[i]=bytes[i]; 
    	int data[] =new int[8];
    	for(int i=0;i<7;i++){
    		data[i] = BytesToInt(b[2*i],b[2*i+1]);
    		if(data[i]>=32768) data[i]=-(65536-data[i]);
    	}
    	data[7] = b[15];
    	for(int x:data) info+=x+"	";
    	//for(byte b:bytes) info+=Byte.toString(b)+",";
    	
    	return info;
    }
    
    private void collectShimmerData() {
    	int shimnum=0;
    	int[] Packet = null;
    	String info = "";
    	for(Shimmer s: shimmers) {
    		if(s.PacketQueue.size() > 0) {
    			//do {
    				//original code
    				//Packet = s.PacketQueue.poll();
    				//new code
    				//Packet = (int[]) s.PacketQueue.toArray()[s.PacketQueue.size()-1];
    				Iterator<int[]> it =s.PacketQueue.iterator();
    				while(it.hasNext()) {
    					Packet =s.PacketQueue.poll();
    					it.next();
    				}
    				
    				// For debugging the queue.
    				//Packet[4] = s.PacketQueue.size();
    				info+="<"+GetPacketBytes(Packet, shimnum)+">";
    				//original code
    				//if(shimmerStream != null) {
    					
    				//	try{shimmerStream.write(GetPacketBytes(Packet, shimnum));}
    				//	catch(IOException e){}
    				//}
    			//}
    			//while(s.PacketQueue.size() > 0);
    			s.PacketQueue.clear();	
        		if(shimnum >= 6)
        			break;
    		}
    		else info+="<	>";
    		shimnum = shimnum+1;
    	}
    	if(shimmerStream != null) {
    		PrintStream ps = new PrintStream(shimmerStream);
    		//Log.w(TAG, "write into the shimmer file.");
    		Date date = new Date();
    		String color;
			if (Global.color == Color.RED)
				color = "RED";
			else
				color = "BLUE";
    		ps.println(String.format(
					"ypos %d  yang %d  %s  x %f  y %f  z %f %s %s",
					Global.ypos-500, Global.yang, sdf.format(date), Global.yaw,
					Global.pitch, Global.roll, color,info));
			}
	}
    
    private int BytesToInt(int x1, int x2){
    	if(x1<0) x1 = 256+x1;
    	if(x2<0) x2 = 256+x2;
    	return x1+x2*256;
    }
    @Override
    public void onBackPressed() {
    }
}


