package com.mhi.acremote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemoteScreen(vm: RemoteViewModel) {
    val s by vm.state.collectAsState()
    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (s.power) "${s.mode} ${s.coercedTemp()}C ${s.fan}" else "OFF", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.togglePower() }) { Text(if (s.power) "Power OFF" else "Power ON") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.setTemp(s.temp - 1) }) { Text("-") }
            Text("${s.coercedTemp()}C", Modifier.padding(horizontal = 16.dp))
            Button(onClick = { vm.setTemp(s.temp + 1) }) { Text("+") }
        }
        Row { Mode.values().forEach { m -> Button(onClick = { vm.setMode(m) }, Modifier.padding(2.dp)) { Text(m.name.take(4)) } } }
        Row {
            Button(onClick = { vm.cycleFan() }) { Text("Fan:${s.fan.name}") }
            Button(onClick = { vm.send(s.copy(swingV = if (s.swingV == SwingV.SWING) SwingV.OFF else SwingV.SWING)) }) { Text("SwingV") }
            Button(onClick = { vm.send(s.copy(threeD = !s.threeD)) }) { Text(if (s.threeD) "3D:ON" else "3D:OFF") }
        }
        Row {
            Button(onClick = { vm.send(s.copy(sleep = !s.sleep)) }) { Text("Sleep") }
            Button(onClick = { vm.send(s.copy(clean = !s.clean)) }) { Text("Clean") }
            Button(onClick = { vm.send(s.copy(swingH = if (s.swingH == SwingH.SWING) SwingH.OFF else SwingH.SWING)) }) { Text("SwingH") }
        }
        Row {
            Button(onClick = { vm.send(s.copy(temp = s.temp)) }) { Text("Timer") }
            Button(onClick = { vm.use152 = !vm.use152 }) { Text(if (vm.use152) "152:ON" else "152:OFF") }
        }
    }
}
