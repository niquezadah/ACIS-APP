# Revisión de la primera versión

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

Abrir el proyecto en Android Studio y seguir el recorrido del README. `smoke.py` es un apoyo para crear una orden de prueba y recorrer el guardado de avance con ADB; agrega datos ficticios y espera un emulador `emulator-5554`. La revisión de cámara, ubicación y cierre se hizo por separado. Las capturas en esta carpeta muestran los estados revisados.

Antes de entregar, probar también en el teléfono que se usará en la defensa: cámara disponible, permisos denegados, ubicación desactivada y legibilidad con tamaño de letra aumentado. No se declara que esos casos hayan sido comprobados en un dispositivo físico.

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

Las pruebas de fotografías están en `app/src/androidTest/java/cl/acis/app/ui/PhotoLoaderTest.kt`. Con un dispositivo de pruebas conectado se pueden ejecutar desde Android Studio o con `gradlew.bat connectedDebugAndroidTest`.

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
