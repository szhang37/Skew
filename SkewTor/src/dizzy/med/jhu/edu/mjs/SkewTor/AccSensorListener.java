package dizzy.med.jhu.edu.mjs.SkewTor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccSensorListener implements SensorEventListener {
     private final SensorManager mSensorManager;
     private final Sensor mAccelerometer;
     //private Context mContext;
     public AccSensorListener(Context context) {
         mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
         mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
         mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
     }

     public void onAccuracyChanged(Sensor sensor, int accuracy) {
     }

     public void onSensorChanged(SensorEvent event) {
        Global.yaw = event.values[0];
        Global.pitch = event.values[1];
        Global.roll = event.values[2];
     	}
     
 }
