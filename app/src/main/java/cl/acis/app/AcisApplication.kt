package cl.acis.app
import android.app.Application
import cl.acis.app.data.LocalOrderRepository
import cl.acis.app.data.EvidenceStore
class AcisApplication: Application() { val evidenceStore by lazy { EvidenceStore(this) }; val repository by lazy { LocalOrderRepository(this) } }
