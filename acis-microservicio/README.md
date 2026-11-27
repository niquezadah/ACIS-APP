# Microservicio REST de ACIS

API REST para consultar, crear y actualizar órdenes de trabajo ACIS. Requiere Java 21; Maven se ejecuta con el wrapper incluido.

## Ejecutar en otro computador

1. Instalar un JDK 21 y comprobar `java -version`.
2. Abrir una terminal en `acis-microservicio`.
3. Iniciar la API:

   - Windows: `mvnw.cmd spring-boot:run`
   - Linux/macOS: `./mvnw spring-boot:run`

La API queda disponible en `http://localhost:8080`. H2 en archivo es la base predeterminada; su archivo se crea bajo `data/` al iniciar la aplicación. PostgreSQL es opcional y puede configurarse con `ACIS_DB_URL`, `ACIS_DB_DRIVER`, `ACIS_DB_USERNAME` y `ACIS_DB_PASSWORD`.

Para compilar y ejecutar las pruebas:

- Windows: `mvnw.cmd clean verify`
- Linux/macOS: `./mvnw clean verify`

Las pruebas de integración usan una base H2 en memoria separada de los datos locales.

## Endpoints

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/api/ordenes` | Lista órdenes, primero las más recientes. |
| GET | `/api/ordenes/{id}` | Obtiene una orden o responde 404. |
| POST | `/api/ordenes` | Crea una orden en estado `ASSIGNED` y responde 201. |
| PUT | `/api/ordenes/{id}` | Actualiza la orden; valida estado, datos y reglas ACIS. |

Los errores usan JSON con `timestamp`, `status`, `error`, `message` y `fieldErrors`. Campos inválidos responden 400, órdenes inexistentes 404 y reglas o transiciones rechazadas 409.

Estados: `ASSIGNED` → `ACCEPTED` → `ACTIVE` → `DONE` → `CLOSED`. No se permiten saltos, retrocesos ni cambios posteriores a `CLOSED`. Para crear se requiere local, dirección, equipo, técnico y motivo de al menos cinco caracteres. Para `DONE` se exigen diagnóstico y trabajo de al menos cinco caracteres y entre una y diez evidencias declaradas. La conformidad requiere un nombre y `customerConfirmed: true`, que se registra mientras el estado está en `DONE`; un `PUT` posterior puede cerrar la orden.

Ejemplo mínimo de creación:

```json
{
  "clientOrderId": "a5796a8e-8497-4cc2-bdbf-44f1ab736c71",
  "site": "Local ACIS",
  "address": "Av. Central 100",
  "equipment": "Equipo 1",
  "technician": "Técnico 1",
  "priority": "Normal",
  "type": "Preventiva",
  "problem": "Mantención preventiva"
}
```

Ejemplo con `curl`:

```sh
curl -i -H "Content-Type: application/json" \
  -d '{"clientOrderId":"a5796a8e-8497-4cc2-bdbf-44f1ab736c71","site":"Local ACIS","address":"Av. Central 100","equipment":"Equipo 1","technician":"Técnico 1","priority":"Normal","type":"Preventiva","problem":"Mantención preventiva"}' \
  http://localhost:8080/api/ordenes
```

`clientOrderId` es un UUID persistente generado por el cliente. Repetir un `POST` con la misma clave y el mismo contenido devuelve la orden existente; reutilizarla con datos distintos responde 409. Los `PUT` deben incluir la misma clave que está asociada a la orden; repetir exactamente una actualización aceptada es seguro, incluso si se perdió la respuesta del cierre.

## Fotografías

El contrato no acepta ni devuelve `photo`, `photos` ni rutas privadas del teléfono; los campos JSON desconocidos se rechazan. `evidenceCount` solo conserva el número declarado para aplicar el límite y exigir evidencia antes de `DONE`; no representa archivos recibidos ni prueba que existan. La carga, transferencia segura, almacenamiento y consulta de imágenes quedan pendientes de un diseño posterior. Android mantiene SQLite como fuente local y conserva allí la asociación de IDs y la cola; la sincronización permanece desactivada desde la interfaz y el ViewModel.
