# PRUEBAS MANUALES — ACIS APP

**Asignatura:** DSY1105 – Desarrollo de Aplicaciones Móviles  
**Institución:** Duoc UC  
**Evaluación:** Parcial 2  
**Proyecto:** ACIS – Gestión de Órdenes de Trabajo en Terreno  
**Responsable de pruebas:** Claudio Córdova  
**Fecha:** 29/10/2026  
**Dispositivo:** Motorola moto g55 5G  
**Rama de trabajo:** `claudio/validacion-cierre`

---

## 1. Objetivo

Verificar el funcionamiento de ACIS en un dispositivo Android físico, comprobando navegación, creación de órdenes, formularios, validaciones, cambios de estado, cámara, conformidad del cliente, cierre de órdenes y persistencia local.

Las pruebas se documentan considerando el caso ACIS, la rúbrica de la Evaluación Parcial 2 y las instrucciones del profesor. Se distingue entre resultados comprobados, verificaciones pendientes y mejoras opcionales. Un resultado satisfactorio en una prueba no acredita por sí solo el cumplimiento completo de un criterio de la rúbrica.

## 2. Pruebas realizadas

| Prueba | Resultado |
|---|---|
| Compilación mediante Gradle | Correcto — BUILD SUCCESSFUL |
| Ejecución de ACIS en Motorola moto g55 | Correcto |
| Navegación por las pantallas examinadas | Correcto |
| Selección de perfiles Jefatura, Técnico y Cliente | Correcto |
| Validación de campos obligatorios al crear órdenes | Correcto |
| Mensajes, bordes e íconos de error al crear una orden | Correcto |
| Creación de OT 0001 | Correcto |
| Cambio Asignada → Aceptada | Correcto |
| Cambio Aceptada → En atención | Correcto |
| Registro de llegada | Correcto |
| Validación de diagnóstico y trabajo realizado | Correcto |
| Validación de fotografía obligatoria | Correcto |
| Captura de fotografía con la cámara | Correcto |
| Visualización de la foto en el detalle de la orden | Correcto |
| Registro de intervención y salida | Correcto |
| Cambio En atención → Por confirmar | Correcto |
| Bloqueo del cierre sin aceptar la conformidad | Correcto |
| Validación de nombre de cliente obligatorio | Correcto |
| Registro de conformidad y cierre definitivo | Correcto |
| Persistencia del estado Cerrada tras reabrir ACIS | Correcto |
| Persistencia de diagnóstico, trabajo, foto y conformidad | Correcto |
| Obtención y registro de ubicación GPS | Pendiente de prueba |
| Verificación específica de animaciones | Pendiente de prueba |

**Resultado general:** se completó satisfactoriamente un recorrido de prueba de la OT 0001, desde su creación hasta su cierre. No se han verificado todos los escenarios ni todos los criterios evaluados.

## 3. Validaciones comprobadas

### 3.1. Creación de órdenes de trabajo

Se intentó crear una orden con campos obligatorios vacíos. ACIS mostró mensajes por campo, bordes rojos e íconos de advertencia.

Se comprobaron las validaciones de:

- Nombre del local obligatorio.
- Dirección obligatoria.
- Equipo obligatorio.
- Técnico asignado obligatorio.
- Motivo de la visita con un mínimo de cinco caracteres.

Después se completó el formulario con información ficticia y se creó la OT 0001.

### 3.2. Registro de intervención

Al intentar finalizar una intervención sin completar los datos requeridos, se mostraron errores de:

- Diagnóstico incompleto (mínimo cinco caracteres).
- Trabajo realizado incompleto (mínimo cinco caracteres).
- Fotografía obligatoria ausente.

Se completaron el diagnóstico y el trabajo realizado y se capturó una fotografía. El campo Materiales permaneció opcional.

La ubicación no fue registrada durante esta prueba; debe evaluarse por separado como segundo recurso nativo.

### 3.3. Validación de conformidad del cliente

La regla de conformidad está implementada en la capa lógica y conectada con el `ViewModel` del proyecto. Se comprobó su comportamiento en el teléfono de la siguiente forma:

1. Se abrió la OT 0001 desde el perfil Cliente, en estado **Por confirmar**.
2. Se ingresó a **Conformidad del cliente**.
3. Sin marcar la aceptación, el botón de cierre permaneció deshabilitado.
4. Se marcó la casilla, manteniendo vacío el nombre de quien confirma.
5. ACIS mostró un borde rojo, un ícono de advertencia y el mensaje **«Ingresa el nombre de quien confirma»**.
6. Se escribió un nombre de prueba y desapareció el error.
7. Con la conformidad marcada y el nombre completo, se presionó **Confirmar y cerrar orden**.
8. La OT 0001 pasó al estado **Cerrada**.

**Resultado:** se verificó el rechazo visual del nombre vacío y el cierre satisfactorio con nombre y conformidad registrados.

### 3.4. Comportamiento visual del formulario de conformidad

Se observó que **Confirmar y cerrar orden** puede aparecer habilitado después de marcar la casilla, aunque el nombre siga vacío. Al intentar continuar, el formulario presenta el mensaje de error y no se observó un cierre inválido.

Se podría mejorar la retroalimentación deshabilitando el botón hasta que todos los datos estén completos. Esta mejora es **opcional**, siempre que la validación efectivamente impida cerrar una orden con datos inválidos.

También se propone reemplazar **«Confirmo mi conformidad con el trabajo»** por **«Estoy conforme con el trabajo realizado»** para evitar redundancia. El cambio de redacción es opcional.

## 4. Persistencia local y estados

### 4.1. Creación y llegada

Se creó la OT 0001 desde el perfil Jefatura. Desde el perfil Técnico se aceptó la orden, se registró la llegada y se inició la atención.

Se reabrió ACIS y se comprobó que la orden conservaba el estado **En atención**, con llegada registrada el **29/10/2026 a las 10:25**.

### 4.2. Intervención y registro de salida

Se registraron los siguientes datos ficticios:

**Diagnóstico:** La cámara frigorífica presenta una puerta mal cerrada.

**Trabajo realizado:** Se ajustó la puerta y se verificó su funcionamiento.

**Materiales:** sin registrar.

**Evidencia:** fotografía capturada con la cámara del teléfono.

En la pantalla se comprobaron:

- **Llegada:** 29/10/2026 a las 10:25.
- **Salida:** 29/10/2026 a las 19:12.
- **Tiempo de atención:** 527 minutos.
- **Estado posterior:** Por confirmar.

El detalle de la orden mostró el diagnóstico, el trabajo realizado y la fotografía.

### 4.3. Conformidad y cierre de la OT 0001

Se utilizó el perfil Cliente para registrar la conformidad. Tras completar la validación del nombre y de la aceptación explícita, la orden pasó de **Por confirmar** a **Cerrada**.

Se verificó que el detalle mostraba:

- Barra de progreso completada.
- Nombre de quien confirmó el trabajo.
- Observaciones opcionales como **Sin registrar**.
- Diagnóstico y descripción del trabajo.
- Fotografía asociada.
- Horarios y tiempo de atención.

**Resultado:** cierre funcional comprobado en el dispositivo físico.

### 4.4. Persistencia después de reabrir ACIS

Después de cerrar la OT 0001 se salió de ACIS y se volvió a abrir la aplicación. La pantalla principal mostró **0 órdenes pendientes** y **1 orden cerrada**.

Al abrir nuevamente la OT 0001, se observó que seguían guardados:

- El estado **Cerrada**.
- El diagnóstico.
- El trabajo realizado.
- La fotografía.
- Las horas de llegada y salida.
- El tiempo de atención.
- El nombre de quien confirmó la conformidad.

**Conclusión:** la prueba demuestra persistencia local de los datos examinados en el Motorola. No sustituye una revisión completa de toda la arquitectura ni de todos los casos de almacenamiento.

## 5. Observaciones y mejoras pendientes

**No todas las observaciones son requisitos obligatorios.** Se clasifican según su relación con la rúbrica, el funcionamiento del proyecto y los acuerdos del equipo.

| Observación | Clasificación | Justificación |
|---|---|---|
| Búsqueda de `OT 0001` sin resultados | Mejora opcional | La rúbrica no exige ese formato concreto de búsqueda. |
| El teclado puede tapar campos | Corrección necesaria si impide completar formularios | Puede afectar la usabilidad, IE 2.1.1; reportado por el equipo. |
| Algunas fotografías aparecen giradas | Corrección recomendable | Puede afectar las evidencias y la integración funcional de cámara; reportado por el equipo. |
| Permitir hasta diez fotografías | Mejora solicitada por el equipo | La rúbrica no exige una cantidad específica; debe coordinarse con el compañero. |
| Botón habilitado con conformidad marcada y nombre vacío | Mejora visual opcional | La validación del campo vacío se muestra y evita continuar sin el nombre. |
| Redacción de la casilla de conformidad | Mejora de redacción opcional | «Estoy conforme con el trabajo realizado» es más breve y natural. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` entre computadores | Problema de entorno de pruebas | Diferencias entre firmas de depuración; no es por sí solo un incumplimiento de la rúbrica. |

### 5.1. Criterio para futuras modificaciones

Se priorizarán, en este orden:

1. Requisitos expresos de la evaluación y del caso ACIS.
2. Correcciones que impidan completar el flujo o afecten un criterio evaluado.
3. Funcionalidades acordadas por ambos integrantes.
4. Mejoras opcionales que no comprometan la estabilidad.

Una mejora no es obligatoria por el solo hecho de haber sido detectada. A la vez, un defecto puede exigir corrección si impide cumplir criterios generales de usabilidad o funcionamiento, aunque no esté descrito literalmente en la rúbrica.

**Estado:** observaciones documentadas; no se consideran corregidas por haber sido registradas aquí.

## 6. Pruebas pendientes

- Obtener y mostrar la ubicación mediante GPS en el Motorola.
- Comprobar los permisos y el comportamiento de la ubicación cuando se deniega el acceso.
- Verificar la conservación de la ubicación, si se registra.
- Revisar la orientación de las fotos en distintas capturas.
- Revisar el comportamiento del teclado al completar formularios.
- Verificar animaciones funcionales y retroalimentación visual (IE 2.2.2).
- Comprobar la adaptabilidad y navegación en distintas condiciones de uso.
- Revisar la evidencia de Trello y la distribución de tareas.
- Comprobar los requisitos de entrega y el README antes de la presentación.

El flujo principal de la OT 0001 **ya fue completado y verificado**; no debe volver a figurar como pendiente.

## 7. Relación con la rúbrica de Evaluación Parcial 2

| Criterio | Ponderación | Evidencia o estado |
|---|---:|---|
| IE 2.1.1 — Diseño y navegación | 15 % | Navegación funcional en las pantallas probadas; adaptabilidad y teclado por revisar. |
| IE 2.1.2 — Formularios validados | 15 % | Mensajes, bordes e íconos comprobados en creación, intervención y conformidad. |
| IE 2.2.1 — Validaciones desde la lógica | 10 % | Reglas centralizadas implementadas; comportamiento observado en las pruebas. |
| IE 2.2.2 — Animaciones funcionales | 10 % | Pendiente de verificación específica. |
| IE 2.3.1 — MVVM y persistencia local | 15 % | Arquitectura presente; persistencia funcional de la OT cerrada y sus datos comprobada. |
| IE 2.3.2 — GitHub y Trello | 20 % | Commits en rama propia; evidencia de Trello pendiente de revisar. |
| IE 2.4.1 — Dos recursos nativos | 15 % | Cámara probada; GPS pendiente de verificar. |

Los resultados constituyen **evidencia parcial de pruebas**, no una nota ni una acreditación automática de todos los indicadores.

## 8. Trabajo colaborativo y control de versiones

**Repositorio:** `https://github.com/niquezadah/ACIS-APP`  
**Rama:** `claudio/validacion-cierre`

### 8.1. Commits verificados

**`2fdaa95`** — `feat: validar conformidad del cliente al cerrar ordenes`

Contribución técnica en `Order.kt`, `AcisViewModel.kt` y `AcisApp.kt` para la validación del cierre de órdenes.

**`543252f`** — `test: documentar pruebas manuales de ACIS en Motorola`

Creación de `verification/PRUEBAS_MANUALES_CLAUDIO.md` y primer registro de pruebas.

La participación de ambos integrantes y la planificación real en Trello deben verificarse también. Este archivo no sustituye los registros de GitHub o Trello.

## 9. Estado general de verificación

**Resultado:** satisfactorio para el flujo principal probado de la OT 0001.

Se comprobaron la creación y aceptación de la orden; el registro de llegada; las validaciones de intervención; la captura y conservación de una fotografía; el registro de salida; la conformidad del cliente; el cierre definitivo; y la persistencia local después de reabrir ACIS.

El recurso nativo de **GPS**, las **animaciones**, ciertos aspectos de **usabilidad** y la evidencia de **Trello** siguen pendientes de comprobación. También queda pendiente preparar la demostración en video y revisar la entrega formal.

Las mejoras opcionales se evaluarán según la rúbrica, el caso ACIS y los acuerdos del equipo, evitando modificaciones innecesarias.

**Conclusión:** ACIS presenta un recorrido principal funcional comprobado en un Motorola moto g55. Aún no corresponde declarar el cumplimiento íntegro de la Evaluación Parcial 2 hasta completar las verificaciones pendientes.
