package mx.cinvestav.emergencias.victima.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.cinvestav.emergencias.victima.App

@Composable
fun LoginScreen(
    onLoginExitoso: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val estado by vm.estado.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(estado.exitoso) {
        if (estado.exitoso) {
            // Recrear el AppContainer con la IP y usuario recién configurados
            // para que el AlertaViewModel conecte al nodo FOG correcto
            (context.applicationContext as App).reiniciarContainer()
            onLoginExitoso()
        }
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
        Text("SAES", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF028090))
        Text(
            "Inicia sesión para continuar",
            fontSize = 13.sp,
            color = Color(0xFF5A7A85),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Iniciar sesión", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = estado.identificador,
                    onValueChange = vm::actualizarIdentificador,
                    label = { Text("Matrícula / No. de empleado") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction    = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = estado.password,
                    onValueChange = vm::actualizarPassword,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (estado.error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(estado.error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = vm::iniciarSesion,
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
                    Text(if (estado.cargando) "Verificando…" else "Ingresar")
                }
            }
        }
    }
}