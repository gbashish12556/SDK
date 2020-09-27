package com.example.sdk

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.widget.Toast


class RotationService: Service(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mRotationSensor: Sensor? = null
    private var service:RotationService? = null
    private var isGeneratorOn = false
    private val SENSOR_DELAY = 500 * 1000 // 500ms
    private var messanger:Messenger? = null
    private val FROM_RADS_TO_DEGS = -57
    val GET_ORIENTATION_FLAG = 111

    inner class OrientationRequestHanlder : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GET_ORIENTATION_FLAG -> {
                    try {
                        messanger = msg.replyTo
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
            }
            super.handleMessage(msg)
        }
    }

    private val orientationMessanger: Messenger = Messenger(OrientationRequestHanlder())

    override fun onBind(intent: Intent?): IBinder? {
        return orientationMessanger.getBinder();
    }

    private val mBinder: IRotation.Stub = object : IRotation.Stub() {
        override fun getRotation(): Int {
            return  Sensor.TYPE_ROTATION_VECTOR
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        isGeneratorOn = true
        try {

            mSensorManager = getSystemService(Activity.SENSOR_SERVICE) as SensorManager
            mRotationSensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            mSensorManager!!.registerListener(this, mRotationSensor, SENSOR_DELAY)

        } catch (e: Exception) {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isGeneratorOn = false

    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
       //
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(isGeneratorOn) {
            if (event!!.sensor === mRotationSensor) {
                if (event!!.values.size > 4) {
                    val truncatedRotationVector = FloatArray(4)
                    System.arraycopy(event!!.values, 0, truncatedRotationVector, 0, 4)
                    update(truncatedRotationVector)
                } else {
                    update(event!!.values)
                }
            }
        }
    }

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
        val pitch = orientation[1] * FROM_RADS_TO_DEGS
        val roll = orientation[2] * FROM_RADS_TO_DEGS
        val senNo: Message = Message.obtain(null, GET_ORIENTATION_FLAG)
        var bundle = Bundle()
        bundle.putString("message","Pitch: $pitch Roll: $roll")
        senNo.data = bundle
        messanger?.send(senNo)
    }
}