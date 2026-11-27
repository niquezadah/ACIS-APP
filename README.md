# ACIS en Terreno

Aplicación Android para coordinar mantenciones de climatización y refrigeración. Jefatura programa la visita, el técnico registra el trabajo y el cliente confirma el resultado. Proyecto DSY1105 de **NICOLÁS QUEZADA** y **CLAUDIO CÓRDOVA**.

[Descargar el APK firmado](entrega/ACIS-en-Terreno.apk)

## Poner en marcha la demostración

1. Abrir Docker Desktop en el computador y esperar a que termine de iniciar.
2. Abrir PowerShell en la carpeta del proyecto y ejecutar `.\iniciar-demo.ps1`. La primera vez se descargan las imágenes y se compilan los servicios. `docker compose ps` muestra los cuatro contenedores. Las dos bases PostgreSQL usan volúmenes que conservan los datos entre reinicios.
3. Abrir el archivo privado `.env` en la raíz del proyecto. Allí están el correo y la contraseña inicial de Administración, generada para esta instalación. Guardar ese archivo en privado. La cuenta inicial se crea al estrenar la base de cuentas.
4. Instalar el APK en un emulador o teléfono de prueba. Para usar USB, activar la depuración USB y aceptar la conexión al computador. Con las herramientas de Android instaladas, ejecutar lo siguiente. Reemplazar `SERIAL` por el identificador de la primera línea de dispositivos, por ejemplo `emulator-5558`.

~~~powershell
adb devices
adb -s SERIAL reverse tcp:8080 tcp:8080
adb -s SERIAL reverse tcp:8081 tcp:8081
adb -s SERIAL install -r entrega/ACIS-en-Terreno.apk
~~~

Android Studio incluye `adb.exe` en `platform-tools` dentro del Android SDK. En Windows se puede encontrar con `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`. Los dos comandos `reverse` conectan el Android con Docker en ese computador. Si se desconecta el cable o reinicia el dispositivo, puede ser necesario repetirlos. Si Android avisa que la firma difiere de la versión instalada, usar un emulador nuevo permite probar sin borrar las órdenes de la instalación anterior.

El APK entregado usa `127.0.0.1:8080` para las órdenes y `127.0.0.1:8081` para las cuentas. Para trabajar fuera de esa conexión hace falta alojar los servicios y compilar con `-PACIS_FIELD_URL=https://dominio-de-ordenes/` y `-PACIS_ACCOUNTS_URL=https://dominio-de-cuentas/`. La app admite HTTP de desarrollo en las direcciones locales indicadas en su configuración de red. Open-Meteo requiere internet.

## Primer recorrido

1. Cada integrante de la demostración entra a **Registrarme**, ingresa su correo, nombre y contraseña y guarda su clave de recuperación. Las cuentas nuevas parten como Cliente sin local asignado.
2. Administración abre **Administrar cuentas**, elige una cuenta y le da el perfil Jefatura.
3. Jefatura inicia sesión y registra un local y una ficha de técnico. Al crear una OT, selecciona el local y registra su equipo, por ejemplo «Condensadora 4» instalada en la azotea. Ingresa la fecha, prioridad, plazo acordado y motivo. Para equipos exteriores, puede indicar las coordenadas del local y consultar lluvia y viento previstos.
4. Administración asigna el perfil Técnico a la cuenta correspondiente y la vincula con su ficha. Vincula la cuenta Cliente con su local. Después de cambiar permisos, la persona vuelve a iniciar sesión.
5. El técnico abre su orden mientras tiene conexión. En terreno puede aceptarla, marcar llegada, registrar diagnóstico, trabajo, materiales, hasta diez fotografías y ubicación, y finalizar la visita. Al volver desde el formulario se guarda el avance. La app conserva la visita en el teléfono y envía los cambios pendientes cuando recupera conexión.
6. El cliente revisa la evidencia de su local y confirma. Jefatura aprueba el cierre. El listado prioriza las abiertas por urgencia y deja las cerradas al final.

**Otras acciones.** Jefatura puede reagendar o reasignar una orden que sigue Asignada, eliminar una orden con confirmación y exportar el informe en CSV. El detalle muestra el historial con el nombre de quien realizó cada acción, la duración y el cumplimiento del plazo. El plazo se mide desde la creación de la orden hasta la llegada del técnico. En el selector de equipos aparece la última mantención cerrada. En **Mi cuenta** se puede cambiar el nombre. **Recuperar** permite elegir otra contraseña con la clave personal y entrega una nueva clave de recuperación.

## Qué se guarda y dónde

- **Android.** Kotlin, Jetpack Compose, Material 3 y ViewModel. SQLite conserva las órdenes descargadas y los cambios pendientes de cada cuenta y asignación. Las fotos quedan en el almacenamiento privado de la app. El GPS se usa al registrar la visita.
- **Servidor.** Retrofit conecta Android con dos servicios Spring Boot. Cada servicio tiene su propia base PostgreSQL en Docker. Cuentas valida la identidad. Órdenes comprueba el perfil y la asignación antes de leer o modificar una visita. Las fotos se envían como archivos y se comprueba su contenido al recibirlas.
- **Sin señal.** El primer ingreso, la creación de órdenes y la administración requieren conexión. Una sesión vigente permite continuar una visita ya descargada. WorkManager programa los envíos pendientes y la pantalla muestra su estado. Cada cambio lleva un identificador para poder reintentarlo. Si dos teléfonos modifican la misma versión, la app ofrece respaldar el avance y sus fotos en ZIP antes de recuperar la versión del servidor.
- **Versión anterior.** Los registros del antiguo selector de perfiles pertenecen a la base de demostración del teléfono. El inicio con cuentas utiliza almacenamiento separado. Las antiguas órdenes carecen de una cuenta propietaria verificada y no se suben automáticamente.

## Servicios y rutas

| Servicio | Rutas | Uso |
| --- | --- | --- |
| Cuentas, puerto 8081 | `POST /accounts/register`, `/accounts/login`, `/accounts/recover`, `/accounts/logout` | Ingreso y recuperación con clave personal. |
| Cuentas | `GET` y `PATCH /accounts/me`, `GET /accounts`, `PUT /accounts/{id}/access` | Perfil y permisos por cuenta. |
| Órdenes, puerto 8080 | `GET` y `POST /field/orders`, `PUT` y `DELETE /field/orders/{id}` | Listar, crear, modificar y eliminar según los permisos. El borrado usa el parámetro `revision`. |
| Órdenes | `GET /field/orders/{id}/history`, `PUT` y `GET /field/orders/{id}/photos/{hash}` | Historial y evidencia de una OT autorizada. |
| Catálogos | `GET /field/catalog`, `PUT /field/catalog/{id}`, `GET /field/equipment?site={id}`, `PUT /field/equipment/{id}` | Locales, técnicos y equipos con su ubicación. |
| Clima | `GET https://api.open-meteo.com/v1/forecast` | Lluvia y viento máximo para el lugar y día de la visita. |

Las solicitudes privadas llevan el token de sesión. Administración concede permisos, Jefatura gestiona la operación, Técnico ve sus asignaciones y Cliente ve su local. El aviso meteorológico se muestra con lluvia prevista o viento desde 40 km/h, un criterio de planificación de esta versión. El pronóstico se atribuye a [Open-Meteo](https://open-meteo.com/en/docs).

## Código, compilación y verificación

- `app/src/main/java/cl/acis/app/model` contiene los datos y reglas de la orden. `viewmodel` coordina las acciones, `ui` reúne las pantallas y `connected` contiene la sesión, el acceso a los servicios, la copia local de órdenes y la sincronización.
- `cuentas` contiene el servicio de usuarios. `acis-microservicio` contiene el servidor de órdenes desarrollado por Claudio y su ampliación para esta entrega.
- Abrir la carpeta raíz en Android Studio, sincronizar Gradle y usar el JDK incluido. Para compilar los servidores fuera de Docker se requiere Java 21 o posterior. Disponemos de pruebas locales con H2 y de un recorrido separado con PostgreSQL.
- La llave JKS de la entrega está en `firma/acis-entrega.jks` en el computador de firma. La configuración de Gradle lee `firma/privada.properties` con la propiedad `password` y usa el alias `acis`. Ambos archivos deben conservarse en un respaldo privado del equipo. El APK del repositorio ya está firmado.

~~~powershell
.\gradlew.bat assembleDebug createDebugUnitTestCoverageReport
python verification/cobertura.py
.\gradlew.bat assembleRelease
.\acis-microservicio\mvnw.cmd -f cuentas/pom.xml package
.\acis-microservicio\mvnw.cmd -f acis-microservicio/pom.xml package
python verification/servicios.py
python verification/postgres.py
~~~

El último comando usa las imágenes construidas por `iniciar-demo.ps1` y crea un proyecto Docker temporal con datos ficticios. El resumen de cobertura cuenta las líneas de modelos, repositorios, ViewModels y lógica de conexión. El informe completo de Gradle también incluye las pantallas. Las verificaciones de entregas anteriores siguen en `verification/`. Para pruebas que instalan aplicaciones en Android, usar un emulador de prueba separado. El [protocolo de verificación](verification/VERIFICACION.md) describe el resguardo de datos.
