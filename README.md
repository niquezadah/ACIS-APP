# ACIS · Órdenes de trabajo en terreno

Primera entrega académica DSY1105. Aplicación Android en Kotlin y Jetpack Compose, con Material 3, MVVM, StateFlow, corrutinas y almacenamiento SQLite local. Diseño azul, celeste y blanco.

## Ejecutar
Abrir esta carpeta en Android Studio, seleccionar el JDK integrado y sincronizar Gradle. Ejecutar `app` en Android 8 (API 26) o superior. Se utiliza compile/target SDK 37, AGP 9.3.3 y Gradle 9.5.0, disponibles en el entorno del curso. `local.properties` es específico de cada computador y no se comparte.

En Windows: `gradlew.bat assembleDebug`, `gradlew.bat testDebugUnitTest`, `gradlew.bat lintDebug`.
APK: `app/build/outputs/apk/debug/app-debug.apk`.

Resultados de compilación, pruebas y recorrido en emulador: [verificación](verification/VERIFICACION.md).

## Recorrido
1. Elegir Jefatura y crear una orden con local, dirección, equipo, técnico, prioridad, tipo y motivo.
2. Elegir Técnico, abrir la orden, aceptarla y registrar llegada.
3. Registrar diagnóstico, actividades y materiales opcionales. Tomar una fotografía y solicitar ubicación actual. Guardar avance o finalizar la visita.
4. Elegir Cliente, revisar el trabajo, ingresar nombre y confirmar conformidad. La orden queda cerrada.
5. Cerrar y volver a abrir: las órdenes, avances guardados y fotografías permanecen en el teléfono.

El usuario confirmó mantener el selector de perfiles para esta entrega. Los perfiles son un selector de demostración, no autenticación ni control de acceso real. Todos los perfiles comparten los datos locales. Usar datos ficticios durante las pruebas.

## Alcance de esta entrega
- Creación, búsqueda y filtros de órdenes; recorrido secuencial hasta cierre.
- Formularios con validación centralizada, mensajes e íconos de error.
- Estado reactivo, indicadores de carga, transiciones de navegación y animación del progreso.
- Persistencia real sin conexión; fotos en almacenamiento privado persistente, compartidas temporalmente con la cámara mediante FileProvider.
- Cámara externa mediante Activity Result y ubicación puntual con permisos. No se requiere seguimiento en segundo plano.
- Conformidad del cliente y tiempo transcurrido entre llegada y salida.
- Diseño con ancho máximo, contenido desplazable y controles Material 3.

Como propuesta de alcance para una primera versión simple (no es una exención confirmada por el docente): no hay servidor, autenticación real, sincronización entre equipos, pronóstico meteorológico, gestión independiente de catálogos ni cálculo de SLA. Los datos de local y equipo se registran dentro de cada orden. Estas funciones del caso final quedan pendientes; esta versión no declara cumplir el caso completo. GitHub/Trello y evidencias de colaboración se prepararán con el equipo posteriormente.

## Organización
`model`: datos y reglas. `viewmodel`: estado y acciones. `data`: contrato de repositorio e implementación local. `ui`: pantallas y componentes. La interfaz del repositorio permite incorporar Room o una fuente remota posteriormente. SQLite almacena cada orden como un agregado JSON versionado por la base; una evolución a catálogos relacionados requerirá migración conservando datos.

## Reglas propuestas para esta versión
Crear requiere local, dirección, equipo, técnico y motivo. Finalizar requiere diagnóstico, trabajo y foto; materiales y ubicación son opcionales para poder continuar si el dispositivo no entrega ubicación. Cerrar requiere nombre y conformidad explícita. Los estados avanzan Asignada → Aceptada → En atención → Por confirmar → Cerrada. Los tiempos usan el reloj del dispositivo y aún no representan auditoría de servidor.

## Fuentes educativas
Guías 1–13 de DSY1105 entregadas por el usuario; caso ACIS y pauta Evaluación Parcial 2. Ejemplos: https://github.com/profealejandroduoc/layoutsKotlin2026 y https://github.com/profealejandroduoc/sintaxis_kotlin_2026.
Documentación: https://developer.android.com/topic/architecture/recommendations , https://developer.android.com/compose , https://developer.android.com/training/basics/intents/result y https://developer.android.com/training/permissions/requesting.

Los nombres de integrantes y evidencias de trabajo colaborativo deben completarse con información real antes de la entrega académica. Respetar la fecha de congelación del repositorio indicada por la pauta.
