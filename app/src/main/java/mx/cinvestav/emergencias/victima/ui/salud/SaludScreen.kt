package mx.cinvestav.emergencias.victima.ui.salud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.cinvestav.emergencias.victima.domain.model.ResultadoRegistro
import mx.cinvestav.emergencias.victima.domain.model.TipoSangre

/**
 * Pantalla del cuestionario de salud (CU-03).
 *
 * Incluye el aviso de privacidad y el checkbox de consentimiento explícito (R8 — LFPDPPP).
 * El botón de guardar solo se habilita si el consentimiento está aceptado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaludScreen(viewModel: SaludViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (state.datosYaRegistrados) "Actualizar datos médicos" else "Mis datos médicos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Esta información solo será visible para brigadistas autorizados durante una emergencia activa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        // --- Tipo de sangre ---
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = state.tipoSangre.etiqueta,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de sangre") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TipoSangre.entries.forEach { tipo ->
                    DropdownMenuItem(
                        text = { Text(tipo.etiqueta) },
                        onClick = {
                            viewModel.onTipoSangreChange(tipo)
                            expanded = false
                        }
                    )
                }
            }
        }

        // --- Alergias ---
        OutlinedTextField(
            value = state.alergias,
            onValueChange = viewModel::onAlergiasChange,
            label = { Text("Alergias conocidas") },
            placeholder = { Text("Ej. penicilina, mariscos, látex") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        // --- Condiciones médicas ---
        OutlinedTextField(
            value = state.condicionesRelevantes,
            onValueChange = viewModel::onCondicionesChange,
            label = { Text("Condiciones médicas relevantes") },
            placeholder = { Text("Ej. diabetes, hipertensión, epilepsia") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        // --- Medicamentos ---
        OutlinedTextField(
            value = state.medicamentos,
            onValueChange = viewModel::onMedicamentosChange,
            label = { Text("Medicamentos actuales") },
            placeholder = { Text("Ej. metformina 500mg, enalapril") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(Modifier.height(4.dp))

        // --- Aviso de privacidad + consentimiento (R8 — LFPDPPP) ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aviso de privacidad", fontWeight = FontWeight.SemiBold)
                Text(
                    "CINVESTAV tratará tus datos médicos con la finalidad exclusiva de " +
                    "brindar atención de emergencia durante siniestros. Solo brigadistas " +
                    "autorizados tendrán acceso a esta información, únicamente mientras " +
                    "exista una emergencia activa. Al finalizar el evento, el acceso se " +
                    "bloqueará automáticamente. Tus datos se almacenan cifrados conforme " +
                    "a la LFPDPPP.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = state.consentimientoAceptado,
                        onCheckedChange = viewModel::onConsentimientoChange
                    )
                    Text(
                        "Acepto el tratamiento de mis datos personales sensibles",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // --- Retroalimentación ---
        when (val res = state.resultado) {
            is ResultadoRegistro.Exito ->
                Text("✅ Datos registrados correctamente.", color = Color(0xFF2E7D32))
            is ResultadoRegistro.Error ->
                Text("❌ ${res.mensaje}", color = MaterialTheme.colorScheme.error)
            else -> {}
        }

        // --- Botón guardar ---
        Button(
            onClick = viewModel::onGuardar,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.consentimientoAceptado &&
                    state.resultado !is ResultadoRegistro.Cargando
        ) {
            if (state.resultado is ResultadoRegistro.Cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (state.datosYaRegistrados) "Actualizar" else "Guardar")
            }
        }

        Spacer(Modifier.height(80.dp)) // Espacio para la barra de navegación inferior.
    }
}
