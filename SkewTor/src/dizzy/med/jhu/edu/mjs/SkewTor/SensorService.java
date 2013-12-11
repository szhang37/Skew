package dizzy.med.jhu.edu.mjs.SkewTor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SensorService extends Service {

	private final IBinder mBinder = new LocalBinder();
	private AccSensorListener sensor;
	private String dataFileName;
	private File file;
	private FileOutputStream dataStream;
    public class LocalBinder extends Binder {
        SensorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SensorService.this;
        }
    }
    @Override
    public void onCreate() {
    	sensor = new AccSensorListener(getApplicationContext());
        // Display a notification about us starting.  We put an icon in the status bar.
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
    	
    	//dataFileName = String.format("skew_Sensor_%f",(float)System.currentTimeMillis()
    	  //  	);
/*    	Log.d("record sensor","start");
    	file = new File("C://a.txt");
    	try {
    		Log.d("record sensor","createFile");
        	file.createNewFile();
        	dataStream = new FileOutputStream(file);
        	Timer timer = new Timer();
        	timer.schedule(new myTimerTask(), Calendar.getInstance().getTime(), 100);
        	}
        	catch(FileNotFoundException e) {
        		Log.d("record sensor","File not found");
        	}
        	catch(IOException e) {
        		Log.d("record sensor","File error");
        	}*/

    	return START_STICKY;
    }
    @Override
    public void onDestroy(){
    	try{
    	dataStream.close();
    	}
    	catch(Exception e){
    		
    	}
    	super.onDestroy();
    }
    class myTimerTask extends TimerTask{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			PrintStream ps = new PrintStream(dataStream);
			ps.println(Global.yaw+":"+Global.pitch+":"+Global.roll);
			Log.d("record sensor",Global.yaw+":"+Global.pitch+":"+Global.roll);
		}
    	
    };
}