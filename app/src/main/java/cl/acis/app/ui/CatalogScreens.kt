package cl.acis.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.acis.app.model.*
import cl.acis.app.viewmodel.*

@Composable internal fun CatalogList(technicians: Boolean, state: CatalogState, vm: CatalogViewModel, edit: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Activos") }
    val title = if (technicians) "Técnicos" else "Locales"
    val entries = if (technicians) state.catalog.technicians.map { CatalogRow(it.id, it.name, it.email, it.active) }
        else state.catalog.sites.map { CatalogRow(it.id, it.name, "${it.client} · ${it.address}", it.active) }
    val visible = entries.filter { (filter == "Todos" || it.active) && (it.name + " " + it.detail).contains(query, true) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Administra los registros disponibles al crear una orden.") }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.readError != null) item { CatalogReadError(state.readError, vm::refresh) }
        item { Button(onClick = { vm.clearErrors(); edit("new") }, enabled = state.loaded && !state.busy && state.readError == null, shape = ControlShape, modifier = Modifier.fillMaxWidth()) { Text(if (technicians) "+ Registrar técnico" else "+ Registrar local") } }
        item { Field("Buscar", query) { query = it }; Choice("Mostrar", listOf("Activos", "Todos"), filter) { filter = it } }
        if (state.loaded && state.readError == null && visible.isEmpty()) item {
            Text(if (entries.isEmpty()) "Aún no hay registros. Agrega el primero para poder crear órdenes." else "No encontramos registros con esta búsqueda o filtro.")
        }
        items(visible, key = { it.id }) { row ->
            Card(onClick = { vm.clearErrors(); edit(row.id) }, enabled = !state.busy, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(row.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(row.detail)
                    Text(if (row.active) "Activo · Toca para editar" else "Inactivo · Toca para editar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private data class CatalogRow(val id: String, val name: String, val detail: String, val active: Boolean)

@Composable internal fun CatalogReadError(message: String, retry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(message)
            TextButton(onClick = retry, shape = ControlShape) { Text("Reintentar lectura") }
        }
    }
}

@Composable internal fun CatalogForm(technicians: Boolean, id: String, state: CatalogState, vm: CatalogViewModel, done: () -> Unit) {
    val technician = state.catalog.technicians.find { it.id == id }
    val site = state.catalog.sites.find { it.id == id }
    if (id != "new" && (if (technicians) technician == null else site == null)) {
        Text("Registro no disponible", Modifier.padding(20.dp)); return
    }
    var name by rememberSaveable(id, technicians) { mutableStateOf(if (technicians) technician?.name.orEmpty() else site?.name.orEmpty()) }
    var email by rememberSaveable(id, technicians) { mutableStateOf(technician?.email.orEmpty()) }
    var address by rememberSaveable(id, technicians) { mutableStateOf(site?.address.orEmpty()) }
    var client by rememberSaveable(id, technicians) { mutableStateOf(site?.client.orEmpty()) }
    var active by rememberSaveable(id, technicians) { mutableStateOf(if (technicians) technician?.active ?: true else site?.active ?: true) }
    val title = if (technicians) "técnico" else "local"
    Page(if (id == "new") "Registrar $title" else "Editar $title", "Los registros inactivos conservan su historial y no aparecen en nuevas órdenes.") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Field(if (technicians) "Nombre del técnico" else "Nombre del local", name, state.errors["name"], enabled = !state.busy) { name = it }
            if (technicians) Field("Correo electrónico", email, state.errors["email"], enabled = !state.busy) { email = it }
            else {
                Field("Cliente o empresa", client, state.errors["client"], enabled = !state.busy) { client = it }
                Field("Dirección", address, state.errors["address"], enabled = !state.busy) { address = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text("Registro activo"); Text("Disponible para nuevas órdenes", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = active, onCheckedChange = { active = it }, enabled = !state.busy,
                    modifier = Modifier.semantics { contentDescription = "Registro activo" })
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(shape = ControlShape, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                val recordId = if (id == "new") "" else id
                if (technicians) vm.saveTechnician(Technician(recordId, name, email, active), done)
                else vm.saveSite(Site(recordId, name, address, client, active), done)
            }) { Text("Guardar registro") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun CatalogSelector(label: String, selectedId: String?, selectedName: String, entries: List<Pair<String, String>>, error: String?, enabled: Boolean, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val available = enabled && entries.isNotEmpty()
    ExposedDropdownMenuBox(expanded = expanded && available, onExpandedChange = { if (available) expanded = it }) {
        OutlinedTextField(
            value = if (selectedId == null) "" else selectedName, onValueChange = {}, readOnly = true,
            enabled = available, label = { Text(label) }, placeholder = { Text("Selecciona una opción") },
            isError = error != null, supportingText = { if (error != null) Text(error) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && available) },
            shape = ControlShape, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = available).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && available, onDismissRequest = { expanded = false }) {
            entries.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { select(id); expanded = false }) }
        }
    }
}
