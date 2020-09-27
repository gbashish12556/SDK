package com.example.sdk

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.os.IBinder


class RotationService: Service(){

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    private val mBinder: IRotation.Stub = object : IRotation.Stub() {
        override fun getRotation(): Int {
            return  Sensor.TYPE_ROTATION_VECTOR
        }
    }
}