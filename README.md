# ACIS · Órdenes de trabajo en terreno

Proyecto de Desarrollo de Aplicaciones Móviles (DSY1105).

Nuestra aplicación permite organizar y registrar visitas de mantención de equipos de refrigeración y aire acondicionado. Reúne el trabajo de jefatura, técnicos y clientes en un recorrido: crear una orden, realizar la visita y confirmar el servicio.

## Integrantes

- NICOLÁS QUEZADA
- CLAUDIO CÓRDOVA

## Funcionalidades

- Creación de órdenes con local, dirección, equipo, técnico asignado, prioridad, tipo de mantención y motivo.
- Búsqueda de órdenes por local, equipo, técnico o número; filtros de pendientes y cerradas. Las abiertas se ordenan por prioridad (Urgente, Alta y Normal) y las cerradas quedan al final. A igual prioridad aparecen primero las más nuevas.
- Aceptación de órdenes y registro de llegada y salida.
- Registro de diagnóstico, trabajo realizado y materiales utilizados.
- Hasta 10 fotografías por orden, tomadas con la cámara o elegidas desde el teléfono.
- Registro de ubicación actual, con permiso del dispositivo, y copia de coordenadas para pegarlas en Google Maps desde cualquier perfil.
- Confirmación del trabajo por parte del cliente.
- Almacenamiento local de órdenes y fotografías para conservar la información al cerrar la app.
- Validaciones con mensajes e íconos, indicadores de carga y animaciones de navegación y progreso.

Las fotografías comparten una altura fija para alinear la galería sin recortar las imágenes. El botón **Eliminar** usa un fondo rojo suave y un icono de basurero. En el listado, el texto de prioridad usa azul, naranja o rojo según su nivel.

La interfaz utiliza Material 3, con colores azul, celeste y blanco, contenido desplazable y ancho limitado en pantallas grandes.

Los botones y campos de texto tienen esquinas de 6 dp. Cámara, galería y ubicación muestran sus iconos correspondientes. Junto a las coordenadas registradas aparece el icono para copiar solo latitud y longitud, sin la precisión del GPS. La prioridad seleccionada se muestra en celeste para Normal, naranja suave para Alta y rojo suave para Urgente. La navegación utiliza un deslizamiento horizontal de 160 milisegundos: la nueva pantalla entra desde la derecha y, al retroceder, sale hacia la derecha mientras la anterior vuelve desde la izquierda. Los formularios ajustan su espacio al teclado para mantener visible el campo activo.

## Cómo usar la aplicación

1. Seleccionar **Jefatura** y crear una orden con los datos de la visita.
2. Seleccionar **Técnico**, abrir la orden, aceptarla y registrar la llegada.
3. Ingresar el diagnóstico y el trabajo realizado. Los materiales son opcionales.
4. Agregar entre 1 y 10 fotografías desde la cámara o con **Elegir del teléfono**. Se pueden revisar deslizando hacia los lados y quitar antes de guardar. Si está disponible, registrar la ubicación. Se puede guardar un avance y continuar después.
5. Finalizar la visita para registrar la salida y dejar el trabajo pendiente de confirmación.
6. Seleccionar **Cliente**, revisar el trabajo, ingresar el nombre de quien confirma y marcar la conformidad para cerrar la orden.

En esta versión, los perfiles se seleccionan desde la pantalla principal para demostrar los tres recorridos. No hay cuentas ni contraseñas y los perfiles comparten los datos del mismo teléfono.

## Estados y validaciones

Las órdenes avanzan en este orden:

**Asignada → Aceptada → En atención → Por confirmar → Cerrada**

- Para crear una orden se requieren local, dirección, equipo, técnico y un motivo de al menos 5 caracteres.
- Para finalizar la visita se requieren diagnóstico y trabajo de al menos 5 caracteres, además de al menos una fotografía (máximo 10).
- Los materiales y la ubicación son opcionales. Si no se obtiene ubicación, se puede continuar con el registro.
- Para cerrar la orden se requiere el nombre de quien confirma y marcar expresamente la conformidad.
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

Las imágenes elegidas desde el selector del sistema se copian al almacenamiento privado para conservarlas aunque el archivo original deje de estar disponible. No se solicita acceso general a la galería. Si la selección supera los cupos disponibles, se agregan solo las imágenes que completan el máximo de 10 y se informa en pantalla.

La cámara se abre mediante Activity Result y recibe acceso temporal al archivo a través de FileProvider. La ubicación se solicita para un registro puntual, sin seguimiento en segundo plano.

`ui/PhotoLoader.kt` interpreta la orientación EXIF que guarda la cámara y reduce el tamaño de la imagen para mostrarla. Esto permite visualizar fotografías verticales, horizontales o reflejadas sin modificar los archivos originales, incluidas las fotos guardadas anteriormente.

## Ejecutar en Android Studio

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

El [registro de verificación](verification/VERIFICACION.md) contiene las comprobaciones realizadas, sus resultados y la relación con la rúbrica. Las capturas están en `verification/` y las pruebas de validación en `app/src/test/`.

Se ha comprobado el recorrido completo en emulador, incluyendo fotografía, ubicación simulada, cierre de la orden y conservación de datos después de reiniciar. La prueba en teléfono físico está pendiente.

## Alcance de esta versión

Esta primera entrega funciona de forma local. Todavía no incluye servidor, cuentas de usuario, sincronización entre dispositivos, pronóstico meteorológico, catálogos independientes de clientes y equipos ni cálculo de plazos de atención (SLA). Los datos del local y del equipo se registran dentro de cada orden.

Estas funcionalidades quedan pendientes para la evolución del proyecto. Guardar información sin conexión no implica sincronizarla con otros teléfonos.

## Material de referencia

- Guías 1–13 de DSY1105, caso ACIS y pauta de Evaluación Parcial 2.
- Ejemplos de clases: [layoutsKotlin2026](https://github.com/profealejandroduoc/layoutsKotlin2026) y [sintaxis_kotlin_2026](https://github.com/profealejandroduoc/sintaxis_kotlin_2026).
- Documentación de Android: [arquitectura](https://developer.android.com/topic/architecture/recommendations), [Jetpack Compose](https://developer.android.com/compose), [Activity Result](https://developer.android.com/training/basics/intents/result) y [permisos](https://developer.android.com/training/permissions/requesting).
