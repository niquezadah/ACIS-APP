package cl.acis.app.data

enum class PendingSyncKind { CREATE, UPDATE }

data class PendingOrderSync(
    val sequence: Long,
    val localOrderId: Long,
    val clientOrderId: String,
    val remoteOrderId: Long?,
    val kind: PendingSyncKind,
    val requestJson: String
)

data class OrderSyncMetadata(
    val clientOrderId: String,
    val remoteOrderId: Long?,
    val eligibleForSync: Boolean
)

/** Local persistence contract for remote identity and queued operations. */
interface OrderSyncStore {
    suspend fun nextPendingSync(): PendingOrderSync?
    suspend fun completePendingSync(pending: PendingOrderSync, remoteOrderId: Long)
    suspend fun pendingSyncCount(): Int
}
