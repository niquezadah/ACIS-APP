---
name: acis-android
description: "Implementar y depurar funcionalidades de ACIS-APP en Kotlin Jetpack Compose Material 3, MVVM/StateFlow y persistencia SQLite; usar para pantallas, validaciones, navegación, cámara, GPS, teclado y fotos en Android Studio, con pruebas en Motorola físico."
---

# ACIS — Desarrollo Android

Leer `references/CLASES_Y_CODIGO.md` para el enfoque del profesor y `../acis-continuidad/references/ESTADO_ACTUAL.md` antes de editar. **Trabajar siempre sobre el repositorio `niquezadah/ACIS-APP`, paquete `cl.acis.app`, no sobre `Android_16` ni el ACIS plantilla**.

## Rutina de desarrollo

1. Verificar raíz de repo, rama, `git status -sb`, cambios locales y código vigente. En máquina de Duoc no asumir que la rama personal esté activa. No pisar cambios de Nicolás.
2. Identificar qué pidió el usuario y qué IE cubre (`acis-rubrica`). Limitarse a lo acordado.
3. Guiar **una acción por turno** y confirmar con texto/captura cuando proceda. Explicar conceptos con lenguaje simple; dar rutas exactas. Entregar archivo completo para pegar si el usuario lo pide, pero sin reescribir archivos enteros innecesariamente.
4. Mantener separación UI (`ui`) / lógica y estado (`viewmodel`) / modelo-reglas (`model`) / persistencia (`data`). Conservar el patrón existente: SQLite local mediante `OrderRepository` (no exigir Room si no se necesita). No reemplazar arquitectura por capricho.
5. Validaciones en lógica (`Rules`, `ViewModel`) y errores en UI por campo. Navegación con `NavController`, `popBackStack()` en vez de crear otro Home para retroceder. Material 3 con jerarquía visual y accesibilidad (`contentDescription`).
6. No agregar dependencias sin necesidad: leer Gradle actual, version catalog si existe y compilar.
7. **Antes de decir terminado:** compilar la raíz correcta (`./gradlew.bat assembleDebug` en Windows), ejecutar prueba funcional y, si aplica, cerrar y reabrir para persistencia. Probar **Motorola moto g55 físico, sin emulador**. Si no se pudo ejecutar, decirlo.

## Errores reales reportados por Nicolás (pendientes)

- Teclado del móvil tapa campos: estudiar IME, `imePadding`, scroll/foco y `windowSoftInputMode` si procede; probar al final del formulario.
- Foto aparece girada: revisar orientación EXIF al visualizar y captura; no destruir evidencia original ni inventar éxito hasta probar.
- Solo una foto por OT: **solicitado hasta 10**. Implica modelo/repositorio/migración y UI, conservar fotos anteriores y persistencia; plan antes de reemplazar `photo: String`.

## Prevenciones

- Las fotos y coordenadas de demostraciones **reales** no se deben compartir en repo público.
- Evitar borrado de datos al cambiar modelo/almacenamiento. No equiparar `rememberSaveable` con persistencia local.
- `BUILD SUCCESSFUL` en `Android_16` prueba solamente `Android_16`; exigir verificación en ACIS-APP.
