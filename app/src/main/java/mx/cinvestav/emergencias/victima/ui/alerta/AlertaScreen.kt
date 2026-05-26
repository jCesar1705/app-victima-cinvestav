package mx.cinvestav.emergencias.victima.ui.alerta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.cinvestav.emergencias.victima.domain.model.EstadoConexion
import mx.cinvestav.emergencias.victima.domain.model.ModoSistema

@Composable
fun AlertaScreen(
    viewModel: AlertaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val enEmergencia = state.modo == ModoSistema.EMERGENCIA
    val colorFondo = if (enEmergencia) Color(0xFFB00020) else Color(0xFF1B5E20)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "App Víctima · CINVESTAV",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text("Conexión con nodo FOG: ${etiquetaConexion(state.conexion)}")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorFondo)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (enEmergencia) "⚠️ EMERGENCIA ACTIVA" else "✓ Modo normal",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                state.alertaActiva?.let { alerta ->
                    Text(alerta.mensaje, color = Color.White)
                } ?: Text(
                    "Datos médicos y ubicación en tiempo real bloqueados.",
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (state.modoPruebaDisponible) {
            Text(
                "Controles de prueba (fuente simulada)",
                style = MaterialTheme.typography.labelLarge
            )
            Button(
                onClick = viewModel::onSimularAlerta,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simular alerta sísmica (CU-08)") }

            OutlinedButton(
                onClick = viewModel::onSimularFin,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simular fin de emergencia") }
        }
    }
}

private fun etiquetaConexion(estado: EstadoConexion): String = when (estado) {
    EstadoConexion.DESCONECTADO -> "Desconectado"
    EstadoConexion.CONECTANDO -> "Conectando…"
    EstadoConexion.CONECTADO -> "Conectado"
}
