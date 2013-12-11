package dizzy.med.jhu.edu.mjs.Skew1;

import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.Date;
import java.text.DateFormat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import dizzy.med.jhu.edu.mjs.Skew1.R;
import android.view.View;

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
