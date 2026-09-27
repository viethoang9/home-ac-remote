package com.mhi.acremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var saveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mgr = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val hasIr = mgr?.hasIrEmitter() == true
        val tx: Transmitter = if (hasIr && mgr != null) AndroidIrTransmitter(mgr) else FakeIrTransmitter()
        val store = PresetStore(applicationContext)
        val vm = RemoteViewModel(tx)
        vm.onPersist = { s, u152 ->
            saveJob?.cancel()
            saveJob = lifecycleScope.launch {
                delay(400)
                runCatching { store.saveLast(s, u152) }
            }
        }
        lifecycleScope.launch {
            runCatching { store.loadLast() }.getOrNull()?.let { (s, u152) ->
                vm.restore(s, u152)
            }
        }
        setContent {
            MaterialTheme {
                if (!hasIr) Text("No IR emitter — Demo mode")
                RemoteScreen(vm)
            }
        }
    }
}
