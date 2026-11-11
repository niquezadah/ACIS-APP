# ACIS en Terreno

Proyecto de Desarrollo de Aplicaciones Móviles (DSY1105).

Nuestra aplicación permite organizar y registrar visitas de mantención de equipos de refrigeración y aire acondicionado. Reúne el trabajo de jefatura, técnicos y clientes en un recorrido: crear una orden, realizar la visita y confirmar el servicio.

## Integrantes

- NICOLÁS QUEZADA
- CLAUDIO CÓRDOVA

## Funcionalidades

- Creación de órdenes con local, dirección, equipo, técnico asignado, prioridad, tipo de mantención y motivo.
- Eliminación de órdenes desde Jefatura, con confirmación previa.
- Búsqueda de órdenes por local, equipo, técnico o número; filtros de pendientes y cerradas. Las abiertas se ordenan por prioridad (Urgente, Alta y Normal) y las cerradas quedan al final. A igual prioridad aparecen primero las más nuevas.
- Aceptación de órdenes y registro de llegada y salida.
- Registro de diagnóstico, trabajo realizado y materiales utilizados, con guardado automático al retroceder desde la intervención.
- Hasta 10 fotografías por orden, tomadas con la cámara o elegidas desde el teléfono.
- Registro de ubicación actual, con permiso del dispositivo, y copia de coordenadas para pegarlas en Google Maps desde cualquier perfil.
- Registro de conformidad del cliente y aprobación final de Jefatura para cerrar la orden.
- Almacenamiento local de órdenes y fotografías para conservar la información al cerrar la app.
- Validaciones con mensajes e íconos, indicadores de carga y animaciones de navegación y progreso.

Las fotografías comparten una altura fija para alinear la galería sin recortar las imágenes. El botón **Eliminar** usa los colores de error del tema y un icono de basurero. En el listado, la prioridad se distingue mediante texto y colores del tema.

La cabecera muestra el logo sin fondo claro exterior, ACIS y SERVICIO EN TERRENO. El título del listado tiene una separación superior de 20 dp. El acceso directo de Android se identifica como ACIS en Terreno y utiliza el mismo logo mediante un icono adaptable. El botón de regreso utiliza el icono de deshacer.

La interfaz utiliza Material 3, una paleta personalizada con esquemas claro y oscuro según el sistema, contenido desplazable y ancho limitado en pantallas grandes.

Los botones y campos de texto tienen esquinas de 6 dp. Cámara, galería y ubicación muestran sus iconos correspondientes. Junto a las coordenadas registradas aparece el icono para copiar solo latitud y longitud, sin la precisión del GPS. La prioridad seleccionada usa los colores terciario para Normal, primario para Alta y de error para Urgente. La navegación utiliza un deslizamiento horizontal de 160 milisegundos: la nueva pantalla entra desde la derecha y, al retroceder, sale hacia la derecha mientras la anterior vuelve desde la izquierda. Los formularios ajustan su espacio al teclado para mantener visible el campo activo.

## Cómo usar la aplicación

1. Seleccionar **Jefatura** y crear una orden con los datos de la visita.
2. Seleccionar **Técnico**, abrir la orden, aceptarla y registrar la llegada.
3. Ingresar el diagnóstico y el trabajo realizado. Los materiales son opcionales.
4. Agregar entre 1 y 10 fotografías desde la cámara o con **Elegir del teléfono**. Se pueden revisar deslizando hacia los lados y quitar antes de guardar. Si está disponible, registrar la ubicación. Se puede guardar un avance y continuar después. Al salir de la intervención con Volver o Atrás, el avance se guarda automáticamente antes de regresar; si falla, el formulario permanece abierto.
5. Finalizar la visita para registrar la salida y dejar el trabajo pendiente de confirmación.
6. Seleccionar **Cliente**, revisar el trabajo, ingresar el nombre de quien confirma, completar las observaciones opcionales y registrar la conformidad. La orden permanece **Por confirmar**.
7. Seleccionar **Jefatura**, revisar la orden y pulsar **Aprobar y cerrar**. Esta acción requiere conformidad registrada.

En esta versión, los perfiles se seleccionan desde la pantalla principal para demostrar los tres recorridos. No hay cuentas ni contraseñas y los perfiles comparten los datos del mismo teléfono.

La pantalla de detalle muestra una línea de tiempo con iconos para Asignada, Aceptada, En atención, Por confirmar y Terminado. El último paso corresponde a una orden Cerrada después de la conformidad del cliente y la aprobación de Jefatura. El guardado automático conserva el estado y los horarios; se realiza al retroceder desde la intervención. El cierre forzado de la aplicación puede dejar cambios del formulario sin guardar.

## Estados y validaciones

Las órdenes avanzan en este orden:

**Asignada → Aceptada → En atención → Por confirmar → Cerrada**

- Para crear una orden se requieren local, dirección, equipo, técnico y un motivo de al menos 5 caracteres.
- Para finalizar la visita se requieren diagnóstico y trabajo de al menos 5 caracteres, además de al menos una fotografía (máximo 10).
- Los materiales y la ubicación son opcionales. Si no se obtiene ubicación, se puede continuar con el registro.
- Para registrar conformidad se requiere el perfil Cliente, un nombre y la aceptación expresa, con la orden Por confirmar.
- Para cerrar se requiere el perfil Jefatura y una orden Por confirmar con conformidad guardada. Las acciones rechazan envíos repetidos.
- Las fechas y el tiempo de atención se calculan con el reloj del dispositivo.

## Tecnologías y organización del código

Utilizamos Kotlin y Jetpack Compose para la interfaz, Material 3 para los componentes visuales, StateFlow para actualizar las pantallas y corrutinas para las operaciones de almacenamiento.

El proyecto sigue MVVM: las pantallas muestran información, el ViewModel administra el estado y las acciones, y el repositorio se encarga de guardar y recuperar los datos.

El código principal está en `app/src/main/java/cl/acis/app/`:

| Archivo o carpeta | Responsabilidad |
| --- | --- |
| `MainActivity.kt` | Inicia la interfaz y conserva el borrador y el perfil al recrear la actividad. |
| `AcisApplication.kt` | Proporciona el repositorio de datos de la aplicación. |
| `model/` | Define las órdenes, sus estados y las reglas de validación. |
| `viewmodel/` | Administra el estado de pantalla y las acciones sobre las órdenes. |
| `data/` | Define el contrato del repositorio y su implementación con SQLite. |
| `ui/` | Contiene las pantallas, navegación, tema y componentes visuales. |

SQLite guarda cada orden como un registro con su contenido en JSON, incluida la lista de fotografías. Las órdenes de versiones anteriores con una sola foto siguen siendo compatibles. Las fotografías se guardan en el almacenamiento privado de la app. La interfaz del repositorio permite cambiar la forma de almacenamiento en futuras versiones; los cambios de estructura deberán incorporar migraciones para conservar los datos.

La lectura separa las órdenes legibles de los registros que no pueden interpretarse y conserva todas las filas originales. El listado distingue una base vacía de una lectura incompleta o fallida, muestra un aviso persistente con opción de reintento y conserva la última lista cargada ante un fallo. Los registros antiguos sin `customerConfirmed` mantienen su estado y usan `false` para esa marca. Durante una lectura incompleta se evita limpiar fotografías que podrían estar referenciadas por registros ilegibles.

Las imágenes elegidas desde el selector del sistema se copian al almacenamiento privado para conservarlas aunque el archivo original deje de estar disponible. No se solicita acceso general a la galería. Si la selección supera los cupos disponibles, se agregan solo las imágenes que completan el máximo de 10 y se informa en pantalla.

La cámara se abre mediante Activity Result y recibe acceso temporal al archivo a través de FileProvider. La ubicación se solicita para un registro puntual, sin seguimiento en segundo plano.

`ui/PhotoLoader.kt` interpreta la orientación EXIF que guarda la cámara y reduce el tamaño de la imagen para mostrarla. Esto permite visualizar fotografías verticales, horizontales o reflejadas sin modificar los archivos originales, incluidas las fotos guardadas anteriormente.

## Ejecutar en Android Studio

Antes de elegir un destino, revisar el [protocolo de protección de datos](verification/VERIFICACION.md#protocolo-de-protección-de-datos). Los pasos de instalación siguientes corresponden a un entorno de desarrollo autorizado. Para recorridos manuales en un dispositivo físico con datos importantes, utilizar la aplicación ya instalada. Cualquier actuación sobre su instalación o datos requiere autorización específica y revisión previa de su efecto.

1. Clonar este repositorio o descargar y descomprimir el proyecto.
2. Abrir la carpeta raíz en Android Studio.
3. Seleccionar un JDK compatible con Gradle 9.5.0; en el entorno de desarrollo se utiliza el JDK integrado de Android Studio.
4. Instalar Android SDK 37 desde SDK Manager y sincronizar Gradle.
5. Seleccionar un emulador o teléfono con Android 8 (API 26) o superior.
6. Ejecutar la configuración `app`.

El proyecto utiliza Android Gradle Plugin 9.3.3, Gradle 9.5.0 y el complemento Compose 2.2.10. El archivo `local.properties` contiene la ruta del SDK de cada computador y no se comparte en el repositorio.

Para comprobar los recursos nativos, el dispositivo debe disponer de una aplicación de cámara y servicios de ubicación. En el emulador se puede configurar una ubicación simulada.

Desde la terminal de Windows, en la raíz del proyecto:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

El APK de prueba se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Pruebas

Las pruebas instrumentadas pueden instalar y desinstalar `cl.acis.app` y `cl.acis.app.test`. Su ejecución en dispositivos con datos importantes está prohibida. Usar exclusivamente un entorno de pruebas desechable identificado y autorizado, con datos ficticios y los dispositivos con datos importantes desconectados, conforme al [protocolo de protección de datos](verification/VERIFICACION.md#protocolo-de-protección-de-datos). Compilación, lint y pruebas unitarias se realizan en el PC según el alcance autorizado.

El [registro de verificación](verification/VERIFICACION.md) contiene las comprobaciones realizadas, sus resultados y la relación con la rúbrica. Las capturas están en `verification/` y las pruebas de validación en `app/src/test/`.

El registro de verificación conserva las pruebas históricas en emulador, con fotografía, ubicación simulada, cierre y persistencia del flujo de esas versiones. Las verificaciones manuales posteriores en dispositivo físico están documentadas en el [registro de pruebas manuales](verification/PRUEBAS_MANUALES_CLAUDIO.md), fechado el 29/10/2026, con su alcance y pendientes. Sus resultados corresponden al flujo anterior de cierre por conformidad.

La revisión local del 11/11/2026 registra 27 pruebas unitarias aprobadas, compilación debug correcta y lint sin errores, con 48 advertencias. Estas comprobaciones cubren reglas, lectura de JSON y estado de la lista. La integración con SQLite y la presentación visual de los nuevos avisos de lectura siguen pendientes en un entorno de pruebas autorizado. Los resultados históricos y locales aportan evidencia parcial para la rúbrica.

## Alcance de esta versión

Esta primera entrega funciona de forma local. Todavía no incluye servidor, cuentas de usuario, sincronización entre dispositivos, pronóstico meteorológico, catálogos independientes de clientes y equipos ni cálculo de plazos de atención (SLA). Los datos del local y del equipo se registran dentro de cada orden.

Estas funcionalidades quedan pendientes para la evolución del proyecto. Guardar información sin conexión no implica sincronizarla con otros teléfonos.

## Material de referencia

- Guías 1–13 de DSY1105, caso ACIS y pauta de Evaluación Parcial 2.
- Ejemplos de clases: [layoutsKotlin2026](https://github.com/profealejandroduoc/layoutsKotlin2026) y [sintaxis_kotlin_2026](https://github.com/profealejandroduoc/sintaxis_kotlin_2026).
- Documentación de Android: [arquitectura](https://developer.android.com/topic/architecture/recommendations), [Jetpack Compose](https://developer.android.com/compose), [Activity Result](https://developer.android.com/training/basics/intents/result) y [permisos](https://developer.android.com/training/permissions/requesting).

## Eliminar una orden

Desde Jefatura, abrir el detalle y tocar Eliminar orden. Se solicita confirmación antes de borrar. Cancelar conserva el registro. Al confirmar se elimina la orden local y, si la lectura está completa, se intenta retirar sus copias privadas de fotografías que no estén referenciadas por otras órdenes; las imágenes originales elegidas desde la galería no se modifican. La eliminación es permanente en este dispositivo. La validación del perfil también se realiza en el ViewModel; los perfiles siguen siendo de demostración.
