package com.android.aidlsdk

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.RemoteException

class MobileSensorEvent : Service(), SensorEventListener {
    private var mRotationSensor: Sensor? = null
    private var pushResultAIDLInterface: PushResultAIDLInterface? = null

    override fun onCreate() {
        try {
            val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY)
            super.onCreate()
        } catch (e: Exception) {
            //device problem
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return object : ActionAIDLInterface.Stub() {
            @Throws(RemoteException::class)
            override fun register(pushResultAIDLInterface: PushResultAIDLInterface) {
                this@MobileSensorEvent.pushResultAIDLInterface = pushResultAIDLInterface
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == mRotationSensor) {
            if (event.values.size > 4) {
                val truncatedRotationVector = FloatArray(4)
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4)
                try {
                    update(truncatedRotationVector)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            } else {
                try {
                    update(event.values)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(RemoteException::class)
    private fun update(vectors: FloatArray) {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors)
        val worldAxisX = SensorManager.AXIS_X
        val worldAxisZ = SensorManager.AXIS_Z
        val adjustedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            worldAxisX,
            worldAxisZ,
            adjustedRotationMatrix
        )
        val orientation = FloatArray(3)
        SensorManager.getOrientation(adjustedRotationMatrix, orientation)
        val x = orientation[0] * FROM_RADS_TO_DEGS
        val y = orientation[1] * FROM_RADS_TO_DEGS
        val z = orientation[2] * FROM_RADS_TO_DEGS
        if (pushResultAIDLInterface != null) {
            pushResultAIDLInterface!!.publishResult(
                " ~~~~ IMU Data (8ms) ~~~~ \n\n          X : $x \n\n          Y : $y \n" +
                        "\n          Z : $z \n" +
                        "\n ~~~~~~~~~~~~~~~~~~~"
            )
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int
    ) {
    }

    companion object {
        private const val SENSOR_DELAY = 1000 * 8 // 8ms
        private const val FROM_RADS_TO_DEGS = -57
    }
}