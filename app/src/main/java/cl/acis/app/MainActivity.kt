package cl.acis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.acis.app.connected.*
import cl.acis.app.model.Order
import cl.acis.app.ui.AcisApp
import cl.acis.app.ui.theme.AppTheme
import cl.acis.app.viewmodel.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException

class MainActivity:ComponentActivity() {
    private var current:AcisViewModel?=null
    private var account:String?=null
    override fun onSaveInstanceState(out:Bundle) {
        current?.let {out.putSerializable("draft",it.state.value.draft);out.putString("account",account)}
        super.onSaveInstanceState(out)
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var session by remember {mutableStateOf(Sessions.load(this))}
            fun change(s:Session?) {Sessions.save(this,s);session=s;current=null;account=s?.profile?.id}
            AppTheme {
                val s=session
                if(s==null) Surface(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {SignIn {change(it)}}
                else key(s.token) {
                    val store=remember { Sessions.store(this,s) }
                    val vm:AcisViewModel=viewModel(key="orders-"+s.token) {
                        AcisViewModel(store,(application as AcisApplication).evidenceStore,store.catalogs).apply {
                            role(s.profile.role)
                            if(savedInstanceState?.getString("account")==s.profile.id) (savedInstanceState.getSerializable("draft") as? Order)?.let(::edit)
                        }
                    }
                    val catalogs:CatalogViewModel=viewModel(key="catalog-"+s.token) {CatalogViewModel(store.catalogs){s.profile.role}}
                    current=vm;account=s.profile.id
                    var status by remember { mutableStateOf("Conectando…") }
                    var conflict by remember {mutableStateOf<Long?>(null)}
                    LaunchedEffect(store) {
                        while(true){
                            if(s.expires<=System.currentTimeMillis()){change(null);break}
                            try {
                                store.sync();conflict=null;vm.retry()
                                status="Al día con el servidor"
                            } catch(e:CancellationException){throw e}
                            catch(e:Exception){
                                if(e is HttpException && e.code()==401){change(null);break}
                                conflict=(e as? SyncConflict)?.orderId
                                val count=store.pending()
                                status=(if(e is SyncConflict) "Hay dos versiones de la OT "+e.orderId+"." else if(e is HttpException) problem(e) else "Sin conexión con el servidor.")+" Cambios pendientes: "+count
                            }
                            delay(12000)
                        }
                    }
                    AcisApp(vm,catalogs,store,status,conflict){change(null)}
                }
            }
        }
    }
}
