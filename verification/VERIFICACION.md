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
| Colaboración (20%) | Pendiente por indicación del usuario: GitHub, Trello y evidencias reales del equipo. |

Esta tabla muestra dónde está implementado cada punto; no garantiza una calificación. El alcance completo del caso ACIS incluye funciones futuras descritas en el README.

## Repetir la revisión

Abrir el proyecto en Android Studio y seguir el recorrido del README. `smoke.py` es un apoyo para crear una orden de prueba y recorrer el guardado de avance con ADB; agrega datos ficticios y espera un emulador `emulator-5554`. La revisión de cámara, ubicación y cierre se hizo por separado. Las capturas en esta carpeta muestran los estados revisados.

Antes de entregar, probar también en el teléfono que se usará en la defensa: cámara disponible, permisos denegados, ubicación desactivada y legibilidad con tamaño de letra aumentado. No se declara que esos casos hayan sido comprobados en un dispositivo físico.
