package mx.cinvestav.emergencias.victima.ui.ipconfig

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun IpConfigScreen(
    onConectado: () -> Unit,
    vm: IpConfigViewModel = viewModel()
) {
    val estado by vm.estado.collectAsState()

    LaunchedEffect(estado.exito) {
        if (estado.exito) onConectado()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "SAES",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF028090)
        )
        Text(
            "Sistema de emergencias sísmicas",
            fontSize = 13.sp,
            color = Color(0xFF5A7A85),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Configurar servidor FOG",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "Ingresa la IP de la laptop que ejecuta el nodo FOG de tu edificio.",
                    fontSize = 12.sp,
                    color = Color(0xFF5A7A85),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = estado.host,
                    onValueChange = vm::actualizarHost,
                    label = { Text("IP del nodo FOG") },
                    placeholder = { Text("ej. 192.168.1.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = estado.port,
                    onValueChange = vm::actualizarPort,
                    label = { Text("Puerto (default 8080)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (estado.error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(estado.error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = vm::conectar,
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (estado.cargando) "Conectando…" else "Conectar")
                }
            }
        }
    }
}
