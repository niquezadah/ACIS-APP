package cl.acis.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.acis.app.data.CatalogRepository
import cl.acis.app.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogState(
    val catalog: Catalog = Catalog(), val loaded: Boolean = false,
    val busy: Boolean = false, val readError: String? = null,
    val errors: Map<String, String> = emptyMap(), val message: String? = null
)

class CatalogViewModel(private val repository: CatalogRepository, private val role: () -> String) : ViewModel() {
    private val mutable = MutableStateFlow(CatalogState())
    val state = mutable.asStateFlow()
    init { refresh() }

    fun refresh() {
        if (state.value.busy) return
        mutable.value = state.value.copy(busy = true)
        viewModelScope.launch {
            try { load() } finally { mutable.value = state.value.copy(busy = false) }
        }
    }

    private suspend fun load() {
        try {
            val catalog = repository.read()
            mutable.value = state.value.copy(catalog = catalog, loaded = true, readError = null)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            mutable.value = state.value.copy(readError = "No se pudieron leer los técnicos y locales. Vuelve a intentarlo.")
        }
    }

    fun clearErrors() { mutable.value = state.value.copy(errors = emptyMap(), message = null) }
    fun dismissMessage() { mutable.value = state.value.copy(message = null) }

    fun saveTechnician(value: Technician, done: () -> Unit) = save(CatalogRules.technician(value), "email", done) {
        repository.saveTechnician(value)
    }

    fun saveSite(value: Site, done: () -> Unit) = save(CatalogRules.site(value), "name", done) {
        repository.saveSite(value)
    }

    private fun save(errors: Map<String, String>, duplicateField: String, done: () -> Unit, write: suspend () -> Unit) {
        if (role() != "Jefatura") {
            mutable.value = state.value.copy(message = "Solo Jefatura puede administrar estos registros.")
            return
        }
        if (state.value.busy) return
        mutable.value = state.value.copy(errors = errors, message = null)
        if (errors.isNotEmpty()) return
        mutable.value = state.value.copy(busy = true)
        viewModelScope.launch {
            try {
                write()
                load()
                mutable.value = state.value.copy(message = "Registro guardado en este teléfono")
                done()
            } catch (e: CancellationException) { throw e }
            catch (_: CatalogConflictException) {
                val message = if (duplicateField == "email") "Ya existe un técnico con este correo" else "Ya existe este local con la misma dirección"
                mutable.value = state.value.copy(errors = mapOf(duplicateField to message))
            } catch (_: Exception) {
                mutable.value = state.value.copy(message = "No se pudo guardar. Tus datos siguen en el formulario; vuelve a intentarlo.")
            } finally { mutable.value = state.value.copy(busy = false) }
        }
    }
}
