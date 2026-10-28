package cl.acis.app
import android.app.Application
import cl.acis.app.data.LocalOrderRepository
class AcisApplication: Application() { val repository by lazy { LocalOrderRepository(this) } }
