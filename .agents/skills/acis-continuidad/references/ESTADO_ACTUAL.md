# ACIS-APP — estado de continuidad

**Actualizado:** 2026-10-29 (comparación GitHub y capturas aportadas en conversación). **Es un corte histórico**: volver a comprobar contra Git y Android Studio al retomar.

## Equipo y repositorios

- Proyecto oficial compartido: `niquezadah/ACIS-APP` (público); principal `main`; paquete Android `cl.acis.app`.
- Nicolás Quezada es responsable del repo, Claudio Córdova (`Clau3333`) colaborador con permiso write verificado en conversación.
- Existe `claudio/validacion-cierre`, commit `2fdaa95`, `feat: validar conformidad del cliente al cerrar ordenes` **publicado** y con seguimiento remoto configurado en el equipo donde se creó.
- Comparación GitHub del 29-10: rama Claudio **1 commit ahead, 0 behind** de `main`; PR todavía no creado. Revisar antes de concluir que sigue igual.
- El commit modifica: `model/Order.kt`, `viewmodel/AcisViewModel.kt`, `ui/AcisApp.kt`. Cambios grandes de formato: 1206 inserciones y 179 eliminaciones (3 archivos); revisar diff para integración.
- Rúbrica: trabajo en GitHub y Trello IE 2.3.2 (20 %); **Trello aún no demostrado** en la conversación.

## Funcionalidad ya codificada pero NO probada integralmente

- `Rules.confirmation(order, accepted)` valida nombre no vacío y conformidad explícita.
- `AcisViewModel.advance` recibe `customerAccepted`, verifica al transicionar a `CLOSED`.
- `AcisApp.kt` envía `customerAccepted = accepted` desde botón de confirmación.
- Existe una compilación previa de ACIS-APP **antes de estos cambios** (`BUILD SUCCESSFUL in 3m40s`, informado 28-10). **No se verificó una nueva compilación ACIS-APP después del commit**.
- `BUILD SUCCESSFUL in 33s; 36 actionable tasks` del 29-10 **pertenece a otro proyecto `Android_16`**, no a ACIS-APP. No contarlo como prueba de ACIS.

## PC de Duoc — observación más reciente

- El usuario manifestó estar en **Duoc, no en su computador personal**.
- En Android Studio abrió `ACIS-APP` dentro de una ruta que parece estar anidada bajo `Android_16` (barra truncada; **verificar carpeta real**). Barra indicó rama `main`, `Add Configuration`, "Gradle project sync in progress...", y mensaje `IDE error occurred`.
- No se demostró que la rama `claudio/validacion-cierre` estuviera abierta ni que Gradle Sync hubiera terminado.
- Prioridad al volver: verificar ruta/branch, evitar ejecutar plantilla `Android_16` y revisar el error/sync antes de cambios. Si se necesita cambiar a rama Claudio en otra máquina, revisar trabajo sin guardar antes de `git fetch` + `git switch`.

## Pruebas/fallos pendientes

1. Compilar **ACIS-APP en rama Claudio** y ejecutar cierre con/sin conformidad en Motorola moto g55 físico (sin emulador).
2. Reporte Nicolás: teclado tapa inputs en móvil; imagen tomada se visualiza girada; una sola foto por OT, solicitado hasta **10**.
3. Pruebas de cámara, ubicación GPS, persistencia al reiniciar, animaciones y navegación como evidencia de rúbrica.
4. Revisar README final, Trello, commits reales de ambos, PR y entrega/AVA según pauta. Fecha límite **pendiente de verificar**.
5. No ampliar al caso final (clima remoto/sincronización) sin delimitar si la entrega parcial lo requiere y acuerdo del equipo.

## Guía de fuentes

- Caso/rúbrica/instrucciones: ver skill `acis-rubrica` y material del profesor `acis-android`.
- Repo oficial: https://github.com/niquezadah/ACIS-APP
- Branch: https://github.com/niquezadah/ACIS-APP/tree/claudio/validacion-cierre
- Commit: https://github.com/niquezadah/ACIS-APP/commit/2fdaa95
