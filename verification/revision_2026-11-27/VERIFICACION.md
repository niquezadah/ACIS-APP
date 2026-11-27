# Técnicos y locales

Rama `codex/tecnicos-y-locales`, creada desde `main` en `f42c4bb`.

## Entorno de verificación

Compilación, lint y pruebas unitarias en el PC. Para las verificaciones de SQLite y recorridos visuales se utiliza únicamente el emulador desechable `ACIS_Review`, destino explícito `emulator-5556`, con registros ficticios. Su AVD está en el directorio de visualizaciones de pruebas. Antes de iniciarlo, ADB no muestra dispositivos conectados. No se instala ni se ejecutan pruebas sobre teléfonos físicos.

La migración se prueba sobre una base independiente generada para cada prueba, nunca sobre la base de una instalación real. Esta comprobación acredita conservación de filas en esa base de prueba, no de datos de un teléfono existente.

## Alcance

- Registros de técnicos y locales, edición y desactivación desde Jefatura.
- Identificadores UUID, correos de técnicos únicos y locales únicos por nombre y dirección.
- Selectores con registros activos, dirección automática y mensajes de listas vacías o errores.
- Comprobación de selección en lógica y persistencia antes de crear una orden.
- Referencias a registros y copia histórica de nombre, dirección y cliente en cada nueva orden.
- SQLite versión 2: agrega tablas sin modificar las filas antiguas de órdenes.
- Los perfiles siguen siendo demostrativos; no hay autenticación ni sincronización remota.

## Resultados

- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug`: correctos.
- 33 pruebas unitarias aprobadas, incluidas 6 nuevas de validación, identificación, registros inactivos y copias históricas.
- 27 pruebas instrumentadas aprobadas en ACIS_Review; 8 nuevas cubren migración desde SQLite 1, persistencia al reabrir, duplicados, desactivación, historial, permisos y errores de lectura o escritura.
- Lint: 0 errores y 46 advertencias. No se modifican las advertencias ajenas a este cambio.
- Recorrido visual con datos ficticios: registro de Ana Soto y Sucursal Centro, selección en los desplegables, dirección automática y creación de una orden con los datos seleccionados. Capturas adjuntas.
- Se comprobó desactivar el técnico, encontrarlo con el filtro Todos y reactivarlo desde la pantalla de edición. Tras detener y volver a abrir la app del emulador, la nueva orden se recuperó usando la búsqueda del listado.
- Pruebas en teléfono físico pendientes. No se acredita conservación sobre una instalación física existente ni sincronización remota.
