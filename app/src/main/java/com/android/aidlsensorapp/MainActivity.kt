package com.android.aidlsensorapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.widget.TextView
import android.widget.Toast
import com.android.aidlsdk.ActionAIDLInterface
import com.android.aidlsdk.MobileSensorEvent
import com.android.aidlsdk.PushResultAIDLInterface

class MainActivity : AppCompatActivity() {
    lateinit var aidlService: ActionAIDLInterface
    lateinit var tvServiceLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvServiceLogs = findViewById(R.id.tvServiceLogs)
        initService()
    }

    //service initialisation
    private fun initService() {
        val intent = Intent(this, MobileSensorEvent::class.java)
        bindService(intent, mConnection!!, Context.BIND_AUTO_CREATE)
    }

    //service release
    private fun releaseService() {
        unbindService(mConnection!!)
        mConnection = null
    }

    private var mConnection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, boundService: IBinder) {
            aidlService = ActionAIDLInterface.Stub.asInterface(boundService)
            Toast.makeText(this@MainActivity, "AIDL service connected", Toast.LENGTH_LONG).show()
            val pushResultAIDL: PushResultAIDLInterface.Stub =
                object : PushResultAIDLInterface.Stub() {
                    override fun publishResult(orientation: String?) {
                        tvServiceLogs.text = orientation
                    }
                }

            try {
                aidlService.register(pushResultAIDL)
            } catch (e: RemoteException) {

            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Toast.makeText(this@MainActivity, "AIDL service disconnected", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseService()
    }
}