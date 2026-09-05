package com.mhi.acremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mgr = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val hasIr = mgr?.hasIrEmitter() == true
        val tx: Transmitter = if (hasIr && mgr != null) AndroidIrTransmitter(mgr) else FakeIrTransmitter()
        val vm = RemoteViewModel(tx)
        setContent {
            MaterialTheme {
                if (!hasIr) Text("No IR emitter — Demo mode")
                RemoteScreen(vm)
            }
        }
    }
}
