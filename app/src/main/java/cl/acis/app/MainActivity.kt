package cl.acis.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.acis.app.ui.*
import cl.acis.app.viewmodel.AcisViewModel
class MainActivity: ComponentActivity() {
 private var currentVm: AcisViewModel? = null
 override fun onSaveInstanceState(outState: Bundle) {
  currentVm?.let { outState.putSerializable("draft",it.state.value.draft);outState.putString("role",it.state.value.role) }
  super.onSaveInstanceState(outState)
 }
 override fun onCreate(savedInstanceState: Bundle?) {super.onCreate(savedInstanceState)
 val repo=(application as AcisApplication).repository
 setContent { AcisTheme { val vm:AcisViewModel=viewModel(factory=object:ViewModelProvider.Factory {
 @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T = AcisViewModel(repo).also { model ->
 (savedInstanceState?.getSerializable("draft") as? cl.acis.app.model.Order)?.let(model::edit)
 savedInstanceState?.getString("role")?.let(model::role)
 } as T
 });currentVm=vm;AcisApp(vm) } }
 }
}