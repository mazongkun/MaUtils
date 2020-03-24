package com.mama.sample.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationUtils {

	private static OrientationUtils instance;
	private SensorManager mSensorManager;
	private AccelerometerSensorListener mAccListener;
	private boolean mHasStarted = false;

    public enum FaceOrientation {
        FACE_ORIENTATION_UP (0x0000001),    // 人脸朝上
        FACE_ORIENTATION_LEFT (0x0000002),  // 人脸朝左
        FACE_ORIENTATION_DOWN (0x0000004),  // 人脸朝下
        FACE_ORIENTATION_RIGHT (0x0000008);  // 人脸朝右

        public final int value;
        FaceOrientation(int value) {
            this.value = value;
        }
    }

	private OrientationUtils() {
	}

	public static OrientationUtils getInstance() {
		if (instance == null) {
			instance = new OrientationUtils();
		}
		return instance;
	}

	/**
	 * 启动加速度传感器
	 */
	public static void start(Context context) {
		OrientationUtils.getInstance().registerListener(context);
	}

	/**
	 * 停止加速度传感器
	 */
	public static void stop() {
		OrientationUtils.getInstance().unregisterListener();
	}

	/**
	 * 根据前后摄像头，依靠重力感应器获取方向后得到 FaceOrientation {@link FaceOrientation}
     */
	public static FaceOrientation getFaceOrientation(boolean isFrontCamera) {
		int dir = getDir(isFrontCamera);
		return getFaceOrientation(dir);
	}

	/**
	 * 根据重力感应器得到的方向映射得到相应的 FaceOrientation {@link FaceOrientation}
     */
	private static FaceOrientation getFaceOrientation(int dir) {
		switch (dir) {
            default:
			case 0:
				return FaceOrientation.FACE_ORIENTATION_UP;
			case 1:
				return FaceOrientation.FACE_ORIENTATION_LEFT;
			case 2:
				return FaceOrientation.FACE_ORIENTATION_DOWN;
			case 3:
				return FaceOrientation.FACE_ORIENTATION_RIGHT;
		}
	}

	/**
	 * 根据重力感应器方向获取到对应的角度(0,90,180,270)
	 */
	public static int getDegree(boolean isFrontCamera){
		return getDir(isFrontCamera) * 90;
	}

	/**
	 * 根据重力感应器方向获取到对应的角度(0,90,180,270)
	 */
	public static int getDegree() {
		return getDir() * 90;
	}

	/**
	 * 根据重力感应器方向获取到对应的方向(0,1,2,3)
	 * @param isFrontCamera 是否为前置摄像头，由于前置摄像头画面是镜像的，所以得到的方向也要做相应的翻转
	 */
	public static int getDir(boolean isFrontCamera) {
		int dir = OrientationUtils.getInstance().getDirection(isFrontCamera);
		return dir;
	}

	/**
	 * 根据重力感应器方向获取到对应的方向(0,1,2,3)
	 */
	public static int getDir() {
		return getDir(false);
	}

	// ------------------------------------------------------------------------------

	/**
	 * Use this method to get the direction of mobile phone
	 * @param isFrontCamera : Input the current camera which is using is front or not
	 * @return the mobile phone direction value
	 */
	private int getDirection(boolean isFrontCamera) {
		if (mAccListener != null) {
			int dir = mAccListener.dir;
			// 当且仅当为前置摄像头且画面是上下方向(portrait、portrait-inverse)的颠倒
			if (isFrontCamera && ((dir & 1) == 1)) {
				dir = (dir ^ 2);
			}
			return dir;
		}
		return -1; // default value is -1
	}
	
	/**
	 * Use this method to start listening of the sensor
	 */
	private void registerListener(Context context) {
		if (mHasStarted) {
			return;
		}
		mHasStarted = true;
		mSensorManager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
		Sensor accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // 获取加速度传感器
		if (accelerometerSensor != null) { // 加速度传感器存在时才执行
			mAccListener = new AccelerometerSensorListener();
			mSensorManager.registerListener(mAccListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL); // 注册事件监听
		}
	}

	/**
	 * Use this method to stop listening of the sensor
	 */
	private void unregisterListener() {
		if (!mHasStarted || mSensorManager == null) {
			return;
		}
		mHasStarted = false;
		mSensorManager.unregisterListener(mAccListener);
	}

	private class AccelerometerSensorListener implements SensorEventListener {
		
		/**
		 * landscape：
		 *  ___________________
		 * | +--------------+  |
		 * | |              |  |
		 * | |              |  |
		 * | |              | O|
		 * | |              |  |
		 * | |______________|  |
		 * ---------------------
		 * portrait：
		 *  ___________
		 * |           |
		 * |+---------+|
		 * ||         ||
		 * ||         ||
		 * ||         ||
		 * ||         ||
		 * ||         ||
		 * |+---------+|
		 * |_____O_____|
		 */
		private int dir = -1;

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				// All values are in SI units (m/s^2)
				float x = event.values[0]; // Acceleration minus Gx on the x-axis
				float y = event.values[1]; // Acceleration minus Gy on the y-axis
				if (Math.abs(x) > 0.5f || Math.abs(y) > 0.5f) {
					if (Math.abs(x) > Math.abs(y)) {
						if (x > 0) {
							dir = 0; // landscape
						} else {
							dir = 2; // landscape-inverse
						}
					} else {
						if (y > 0) {
							dir = 1; // portrait
						} else {
							dir = 3; // portrait-inverse
						}
					}
				}
			}
		}
	}
	
}
