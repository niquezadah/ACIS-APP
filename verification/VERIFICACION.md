# Verificación de ACIS

## Protocolo de protección de datos

Vigente desde el 11 de noviembre de 2026. Rige las verificaciones futuras; las secciones posteriores conservan los resultados históricos y sus dispositivos de origen.

Las pruebas instrumentadas pueden instalar y desinstalar los paquetes de aplicación y de pruebas. Deben ejecutarse en un entorno de pruebas desechable con datos ficticios. Una prueba aprobada sobre una base temporal no acredita conservación de los datos de una instalación existente.

### Comprobaciones según el alcance autorizado

| Comprobación | Entorno y condición |
| --- | --- |
| Documentación y configuración de instrucciones | Lectura, revisión de diff y Git, validación de sintaxis en memoria. |
| Compilación, lint y pruebas unitarias | PC. Seleccionar tareas explícitas como `:app:assembleDebug`, `:app:lintDebug` y `:app:testDebugUnitTest`. |
| Recorrido manual y recursos nativos físicos | Dispositivo físico con la aplicación ya instalada y órdenes ficticias identificadas. Conservar los registros existentes. |
| Pruebas instrumentadas de SQLite, ViewModel o UI | Exclusivamente un entorno desechable identificado y autorizado, con bases de prueba independientes y datos ficticios. |

### Preparación de pruebas instrumentadas

1. Documentar el entorno de pruebas desechable y su identificador antes de autorizar su preparación y ejecución. Puede ser un emulador dedicado o un dispositivo de pruebas sin datos importantes.
2. Desconectar los dispositivos con datos importantes antes de ejecutar instrumentadas. Confirmar el destino en Android Studio o en el ejecutor empleado. El uso de una base temporal dentro de una prueba no aísla el ciclo de instalación del paquete.
3. Las tareas `connectedAndroidTest`, `connectedDebugAndroidTest`, sus variantes y la ejecución instrumentada desde Android Studio pueden instalar y desinstalar la aplicación y su APK de pruebas. Su ejecución en dispositivos con datos importantes está prohibida.
4. Ante fallos de firma, instalación o lectura, detener la operación y registrar el error. No usar desinstalación, `adb uninstall`, `pm clear`, restablecimiento de datos ni cambios de `applicationId` como reparación automática. Toda actuación sobre la instalación o los datos del teléfono requiere autorización específica y revisión previa de su efecto.
5. Una instalación con `-r` no acredita conservación si el paquete anterior ya fue eliminado. Separar la evidencia de persistencia de bases de prueba de la conservación de una instalación real.

### Recorrido manual en un dispositivo físico

Comprobar primero desde la interfaz instalada qué órdenes existen y cuál es la orden ficticia del recorrido. Registrar el alcance autorizado y conservar los datos anteriores. Si faltan órdenes o hay un error de lectura, detener el recorrido y solicitar un diagnóstico de solo lectura. Antes de cualquier actuación sobre la instalación o el almacenamiento, definir una protección o copia recuperable de los datos y obtener autorización específica.

Informar expresamente toda prueba pendiente. Las revisiones documentales se verifican mediante lecturas y no autorizan pruebas, instalaciones, operaciones ADB ni preparación de dispositivos. Mantener activas las solicitudes de autorización para cambios de archivos y las confirmaciones del entorno.

## Lectura de órdenes y verificaciones locales — 11 de noviembre de 2026

El código separa la conformidad del Cliente de la aprobación final de Jefatura. La conformidad deja la orden Por confirmar; Jefatura puede cerrarla cuando la conformidad está guardada.

La lectura de SQLite interpreta cada fila de forma independiente, conserva los registros originales e informa los IDs ilegibles. El estado de la interfaz distingue una base vacía de una lectura parcial o fallida y mantiene un aviso con reintento. Si falla una lectura, se conserva la última lista cargada. La versión SQLite continúa en 1 y se admite la ausencia de `customerConfirmed` en registros antiguos.

La ejecución local registrada para esta revisión terminó con los siguientes resultados.

- `:app:testDebugUnitTest` aprobó 27 pruebas, sin fallos ni errores. Incluye 8 pruebas de lectura de JSON, 5 de estado de lectura y las 14 pruebas anteriores de reglas y presentación.
- `:app:assembleDebug` terminó correctamente.
- `:app:lintDebug` terminó sin errores, con 48 advertencias. Una advertencia informa de una versión posterior de la dependencia JSON utilizada exclusivamente para pruebas locales.
- La cobertura nueva comprueba filas inválidas, estados desconocidos, registros antiguos y conservación de referencias a fotografías, ubicación, horarios y la marca de conformidad.

Quedan pendientes la comprobación de integración con SQLite y la revisión visual de los nuevos avisos en un entorno de pruebas autorizado. Estas pruebas locales no acceden a los datos de un dispositivo físico.

La revisión aporta evidencia para IE 2.2.1, reglas y estado reactivo, e IE 2.3.1, arquitectura y persistencia local, según el resumen de rúbrica disponible. El cumplimiento completo requiere contrastar la pauta oficial y completar la verificación funcional pendiente.

## Revisión de la primera versión

Revisión realizada entre el 27 y el 28 de octubre de 2026. Datos ficticios. Pruebas en emulador Android; aún no se ha probado en un teléfono físico.

## Comprobaciones realizadas

- `assembleDebug`: APK generado correctamente.
- `testDebugUnitTest`: 7 pruebas aprobadas, sin errores. Cubren campos obligatorios, evidencia y avance secuencial de estados.
- `lintDebug`: sin errores; 6 avisos de versiones más recientes de herramientas y bibliotecas. Se conservaron las versiones que compilan en este entorno.
- Creación de una orden y recuperación después de cerrar y abrir la app.
- Aceptación, registro de llegada, diagnóstico, trabajo y guardado de avance.
- Apertura de la cámara del emulador, captura y retorno de la fotografía al formulario.
- Solicitud de permiso y registro de ubicación simulada en Santiago. No representa una medición física.
- Registro de salida y cierre mediante nombre y conformidad del cliente.
- Conservación del estado Cerrada tras instalar el APK actualizado y reiniciar.
- Filtros Pendientes y Cerradas; fotografía y nombre del cliente conservados tras reinicio.
- Formulario vacío: mensajes e íconos de error.
- Giro de pantalla: el texto del borrador se conserva y el formulario sigue siendo desplazable.

## Relación con la pauta

| Criterio | Implementación |
| --- | --- |
| Interfaz y navegación (15%) | Material 3; listado, creación, detalle, intervención y conformidad; regreso entre pantallas. |
| Formularios (15%) | Validación de campos, mensajes e íconos, campos opcionales identificados. |
| Estado y validaciones (10%) | StateFlow y reglas separadas de las pantallas. |
| Animaciones (10%) | Transiciones entre pantallas, progreso de la orden y aparición de indicadores y errores. |
| Organización y persistencia (15%) | MVVM, repositorio sustituible, SQLite y fotografías privadas. |
| Recursos nativos (15%) | Cámara y ubicación con solicitud de permiso. |
| Colaboración (20%) | Repositorio publicado en GitHub. Pendientes: planificación en Trello y evidencias de participación de ambos integrantes. |

Esta tabla muestra dónde está implementado cada punto; no garantiza una calificación. El alcance completo del caso ACIS incluye funciones futuras descritas en el README.

## Repetir la revisión

Para futuras revisiones, aplicar primero el protocolo de protección anterior. `smoke.py` y `device.py` son apoyos históricos que escriben datos y manejan la interfaz mediante ADB en `emulator-5554`. Su ejecución requiere autorización y confirmación de un entorno desechable. La revisión histórica de cámara, ubicación y cierre se hizo por separado. Las capturas de esta carpeta corresponden a esos recorridos.

Antes de entregar, planificar un recorrido manual autorizado en el teléfono de la defensa con la aplicación instalada, cámara, permisos denegados, ubicación desactivada y letra aumentada. Registrar qué escenarios se verificaron y cuáles siguen pendientes.

## Revisión de fotos e interfaz — 29 de octubre de 2026

Cambios sobre `main`:

- La visualización interpreta la orientación EXIF, incluidas las fotografías ya guardadas, sin cambiar el archivo original. Se reduce la resolución de visualización para limitar el uso de memoria.
- Los formularios reservan espacio para el teclado y desplazan el campo activo para mantenerlo visible.
- La navegación usa deslizamiento horizontal de 160 ms, con dirección inversa al volver.
- Botones y selectores comparten esquinas de 8 dp. La prioridad Alta seleccionada usa naranja suave y Urgente usa rojo suave; Normal conserva el celeste.

Verificaciones de esta revisión:

- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug` completados. Lint: 0 errores y 7 advertencias.
- Las 7 pruebas de reglas de negocio pasan.
- Las 4 pruebas de `PhotoLoaderTest` pasan en emulador Android: las ocho orientaciones EXIF con comprobación de esquinas y dimensiones; fotografía sin orientación; reducción de imagen grande; archivos ausentes o dañados. También se verifica que el archivo original no se modifique.
- Creación de orden y navegación comprobadas en el emulador. Se revisaron visualmente los colores Alta/Urgente y el campo Motivo con texto y teclado abierto, completamente visible sobre el teclado.
- Capturas de esta revisión en `revision_2026-10-29/`. La comprobación de estos cambios en un teléfono físico queda pendiente.

Las pruebas de fotografías están en `app/src/androidTest/java/cl/acis/app/ui/PhotoLoaderTest.kt`. Su ejecución instrumentada queda sujeta al protocolo vigente de entorno de pruebas desechable y dispositivos con datos importantes desconectados.

## Ampliación a diez fotografías — 29 de octubre de 2026

- Hasta diez fotos por orden, sumando cámara y selector del teléfono. Se pueden revisar horizontalmente y quitar durante la intervención.
- Al elegir más imágenes que los cupos disponibles, se agregan solo las necesarias para completar diez y se informa en pantalla.
- La lista se guarda en el contenido JSON de la orden. La lectura conserva compatibilidad con órdenes antiguas que solo tienen el campo `photo`.
- Las imágenes elegidas se copian al almacenamiento privado, sin depender de la disponibilidad posterior del archivo de origen.
- Las transiciones de regreso usan la dirección inversa a las de entrada, ambas de 160 ms.

Comprobaciones: compilación y revisión Android sin errores; 10 pruebas de reglas de negocio y 8 pruebas en emulador aprobadas. Estas últimas cubren orientación, importación, persistencia de diez fotos, eliminación de referencias, compatibilidad con datos antiguos y rechazo de una orden con once fotos.

Prueba del selector: selección de once imágenes ficticias, recepción de diez y aviso de límite. Se quitó una foto, se tomó otra con la cámara y se guardó el avance. Las diez fotos permanecieron después de reiniciar la app. Se comprobó el regreso desde el detalle mediante Volver y el botón Atrás de Android. Estas pruebas no sustituyen la revisión en teléfono físico.


## Ajustes de galería y retroceso de Android

Las fotografías ocupan un marco de 240 dp de alto, con ajuste completo sin recorte, para alinear los botones independientemente de su orientación. Se retiraron los rótulos numerados y el botón Eliminar utiliza fondo rojo suave e icono de basurero. El texto de prioridad en las tarjetas refleja su nivel.

El retroceso dentro de la app se centraliza con BackHandler después del NavHost para utilizar la misma acción que Volver. La pantalla inicial conserva el comportamiento de salida de Android y el teclado conserva su manejo de retroceso.

Validación: assembleDebug, testDebugUnitTest y lintDebug completados sin errores; 10 pruebas de reglas aprobadas. En emulador se comprobó el botón Atrás desde el detalle hasta el listado y la eliminación de una foto desde la intervención. La revisión de la animación en el teléfono físico queda pendiente.


## Iconos, coordenadas y orden de atención

Se agregaron los iconos de cámara, galería, pin y copiar; cámara y galería usan celeste suave. Botones y campos comparten esquinas de 6 dp. El botón de copiar está disponible en el detalle para todos los perfiles, la intervención y la conformidad cuando hay coordenadas válidas. Copia únicamente latitud y longitud para su uso en Google Maps.

El listado ordena las órdenes abiertas por Urgente, Alta y Normal. Las cerradas quedan al final; a igualdad de prioridad se muestra primero la orden más nueva.

Validación: compilación y lint sin errores; 14 pruebas unitarias aprobadas, incluidas cuatro para ordenamiento y formato de coordenadas. En emulador se revisaron los iconos y se copió una ubicación simulada desde intervención y detalle; al pegar en un campo se verificó el texto exacto sin la precisión GPS. La revisión en teléfono físico queda pendiente.


## Línea de tiempo y guardado al retroceder

La barra de progreso del detalle se reemplaza por cinco pasos con iconos. Se distinguen los estados alcanzados, el actual y los pendientes. Terminado representa el estado Cerrada tras la conformidad del cliente; no se cambian los estados almacenados de órdenes anteriores.

Al retroceder desde la intervención con Volver o Atrás se guarda el borrador antes de navegar. Se permiten campos incompletos y no se registra salida ni se finaliza la orden. Si hay una importación de fotos en curso, se espera su término antes de guardar. Si falla el almacenamiento, se conserva el formulario y se informa el error. Esto no representa guardado al cerrar forzosamente la aplicación.

Compilación y lint sin errores. Pasan las 14 pruebas unitarias y 11 pruebas en Android. Las tres nuevas pruebas verifican guardado de visita incompleta, permanencia en el formulario cuando falla el guardado y regreso sin escritura adicional cuando no hay cambios.

Recorrido en emulador: se ingresó texto sin usar Guardar avance y se salió primero con Volver y luego con Atrás de Android. Ambos cambios se conservaron; se reinició la app y se recuperó el texto. La línea de tiempo se revisó visualmente en En atención. Captura en revision_2026-11-05.


## Identidad visual y eliminación de órdenes — 6 de noviembre de 2026

Se integra el logo proporcionado en la cabecera azul y en un icono adaptable de Android. El nombre de la aplicación pasa a ACIS en Terreno. La cabecera muestra SERVICIO EN TERRENO y el regreso utiliza el icono undo. Se retira el eslogan del listado.

Jefatura puede eliminar una orden desde el detalle después de una confirmación. El ViewModel también comprueba el perfil. Se conserva la orden si falla el borrado. Después de eliminar se retira del listado y se intenta limpiar su evidencia privada no compartida por otras órdenes.

Pasaron las 14 pruebas unitarias y 15 pruebas en Android. Las cuatro nuevas verifican rechazo de eliminación desde Técnico y Cliente, eliminación de la orden seleccionada, conservación en caso de error y persistencia del borrado al reabrir la base. Se probó en pantalla cancelar el diálogo y luego confirmar la eliminación de una orden ficticia, conservando la otra. Se comprobó en el APK el nombre ACIS en Terreno. Revisión física pendiente.


## Logo transparente y separación del listado

Se reemplaza la imagen usada en cabecera e icono adaptable por una versión PNG con transparencia exterior, conservando el distintivo azul. El listado incorpora 20 dp de margen superior antes del título. Verificación: assembleDebug y lintDebug completados sin errores; canal alfa comprobado y aspecto revisado en emulador.
