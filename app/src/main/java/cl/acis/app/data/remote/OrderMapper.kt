package cl.acis.app.data.remote

import cl.acis.app.model.Order

/** Para POST se omite el ID: el backend asigna la identidad remota. */
fun Order.toCreateRequestDto(clientOrderId: String): OrderRequestDto =
    toRequestDto(remoteId = null, clientOrderId = clientOrderId)

/** La cola conserva el ID local; el coordinador agrega el ID remoto al enviar el PUT. */
fun Order.toUpdateRequestDto(clientOrderId: String): OrderRequestDto =
    toRequestDto(remoteId = null, clientOrderId = clientOrderId)

private fun Order.toRequestDto(remoteId: Long?, clientOrderId: String): OrderRequestDto {
    require(clientOrderId.isNotBlank()) { "La clave de orden del cliente es obligatoria" }
    return OrderRequestDto(
        id = remoteId,
        clientOrderId = clientOrderId,
        site = site,
        address = address,
        equipment = equipment,
        technician = technician,
        priority = priority,
        type = type,
        problem = problem,
        stage = stage,
        diagnosis = diagnosis,
        work = work,
        materials = materials,
        location = location,
        arrival = arrival,
        departure = departure,
        customer = customer,
        feedback = feedback,
        customerConfirmed = customerConfirmed,
        evidenceCount = evidencePhotos.size
    )
}

/** Conserva la identidad SQLite entregada por quien invoque el mapeo. */
fun OrderResponseDto.toRemoteOrder(localId: Long = 0): RemoteOrder = RemoteOrder(
    remoteId = id,
    clientOrderId = clientOrderId,
    order = Order(
        id = localId,
        site = site,
        address = address,
        equipment = equipment,
        technician = technician,
        priority = priority,
        type = type,
        problem = problem,
        stage = stage,
        diagnosis = diagnosis,
        work = work,
        materials = materials,
        location = location,
        arrival = arrival,
        departure = departure,
        customer = customer,
        feedback = feedback,
        customerConfirmed = customerConfirmed
        // El servidor solo devuelve evidenceCount: no crea ni reconstruye rutas de fotos.
    ),
    evidenceCount = evidenceCount
)
