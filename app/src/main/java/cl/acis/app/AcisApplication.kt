package cl.acis.app
import android.app.Application
import cl.acis.app.data.LocalOrderRepository
import cl.acis.app.data.EvidenceStore
import cl.acis.app.data.LocalCatalogRepository
class AcisApplication: Application() { val evidenceStore by lazy { EvidenceStore(this) }; val repository by lazy { LocalOrderRepository(this) }; val catalogRepository by lazy { LocalCatalogRepository(repository) } }
