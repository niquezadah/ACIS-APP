package cl.acis.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.acis.app.R
import cl.acis.app.model.Stage

@Composable
internal fun OrderTimeline(stage: Stage) {
    val blue = MaterialTheme.colorScheme.primary
    val pale = MaterialTheme.colorScheme.primaryContainer
    val inactive = Color(0xFFD6E1EA)
    val labels = listOf("Asignada", "Aceptada", "En atención", "Por confirmar", "Terminado")
    val icons = listOf(R.drawable.ic_stage_assigned, R.drawable.ic_stage_accepted,
        R.drawable.ic_stage_active, R.drawable.ic_stage_pending, R.drawable.ic_stage_finished)
    val progress by animateFloatAsState(stage.ordinal / 4f, label="timeline")
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Estado de la orden", style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                val start = size.width / 10f
                val length = size.width * 0.8f
                drawLine(inactive, Offset(start,size.height/2), Offset(start+length,size.height/2), 3.dp.toPx())
                drawLine(blue, Offset(start,size.height/2), Offset(start+length*progress,size.height/2), 3.dp.toPx())
            }
            Row(Modifier.fillMaxWidth()) {
                Stage.entries.forEachIndexed { index, item ->
                    val reached = index <= stage.ordinal
                    val current = item == stage
                    Column(Modifier.weight(1f), horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Surface(shape=CircleShape, color=if(reached) pale else MaterialTheme.colorScheme.surface,
                            modifier=Modifier.size(44.dp).border(if(current) 2.dp else 1.dp, if(reached) blue else inactive, CircleShape)) {
                            Box(contentAlignment=Alignment.Center) {
                                Icon(painterResource(icons[index]),contentDescription=null,
                                    tint=if(reached) blue else Color(0xFF7B8D9A),modifier=Modifier.size(23.dp))
                            }
                        }
                        Text(labels[index],textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall,
                            fontWeight=if(current) FontWeight.Bold else FontWeight.Normal,
                            color=if(reached) blue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier=Modifier.fillMaxWidth().padding(horizontal=2.dp))
                    }
                }
            }
        }
        Text("Paso ${stage.ordinal+1} de 5 · ${labels[stage.ordinal]}",color=blue,style=MaterialTheme.typography.labelMedium)
    }
}
