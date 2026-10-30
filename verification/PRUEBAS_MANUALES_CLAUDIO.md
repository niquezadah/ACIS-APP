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

Verificar el funcionamiento de ACIS en un dispositivo Android físico, comprobando navegación, creación de órdenes, formularios, validaciones, cambios de estado, cámara, ubicación del dispositivo, animaciones funcionales, conformidad del cliente, cierre de órdenes y persistencia local.

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
| Creación y aceptación de la OT 0002 | Correcto |
| Registro de llegada de la OT 0002 | Correcto |
| Obtención y visualización de ubicación del teléfono en la OT 0002 | Correcto |
| Guardado de ubicación mediante «Guardar avance» | Correcto |
| Persistencia de ubicación tras cerrar y reabrir ACIS | Correcto |
| Animación de carga al obtener ubicación | Correcto, observada en el teléfono |
| Barra de progreso según el estado de la OT | Correcto — animación verificada visualmente por quien realizó la prueba en Motorola |

**Resultado general:** se completó satisfactoriamente el recorrido de la OT 0001 desde su creación hasta su cierre. En la OT 0002 se comprobaron la creación, aceptación, llegada, ubicación del dispositivo y persistencia del dato, además de la animación de carga del GPS. El responsable también confirmó haber observado la animación de la barra de progreso de las órdenes. No se han verificado todos los escenarios ni todos los criterios evaluados.

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

La ubicación no fue registrada durante esta primera prueba de la OT 0001. Su obtención y conservación se comprobaron posteriormente en la OT 0002, según el apartado 4.5.

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

### 4.5. OT 0002 — Ubicación del dispositivo y persistencia

**Fecha:** 29/10/2026  
**Dispositivo:** Motorola moto g55 5G  
**Perfil:** Jefatura para crear la OT y Técnico para registrar la visita  
**Orden:** OT 0002, Sucursal ACIS de Prueba  
**Resultado:** satisfactorio para el escenario probado.

Se creó la OT 0002 con datos ficticios de un mantenimiento preventivo de la cámara frigorífica CF-001. Se aceptó la orden y se registró la llegada el **29/10/2026 a las 20:56**, quedando en estado **En atención**.

Desde **Registrar intervención**, se utilizó **Registrar ubicación actual**. ACIS mostró un resultado de ubicación del dispositivo y una precisión indicada de aproximadamente **±182 metros**. No se incluyen las coordenadas reales en este documento por privacidad.

Luego se presionó **Guardar avance**, se comprobó el mensaje **«Guardado en este teléfono»** y se observó **«Ubicación registrada»** en el detalle de la orden. Posteriormente se cerró y reabrió la aplicación, y la ubicación seguía registrada en la OT 0002.

**Resultado:** se comprobó la obtención, visualización, guardado y persistencia de la ubicación en esta orden. La exactitud espacial fue limitada; no se ha evaluado la ubicación con permisos denegados ni en otros entornos.

**Estado final de la OT 0002 al terminar esta prueba:** En atención, sin registrar salida. Se conserva para continuar las pruebas o la demostración.

### 4.6. Animaciones funcionales

En el formulario **Registrar intervención**, al presionar **Registrar ubicación actual**, se observó una **barra de carga animada** mientras ACIS consultaba la ubicación. El usuario confirmó visualmente su aparición en el Motorola.

En el detalle de la OT 0002 se observó una barra horizontal de progreso correspondiente al estado **En atención**. El código utiliza `animateFloatAsState` para cambiar su valor cuando cambia la etapa. **El responsable de la prueba confirmó que observó visualmente el movimiento de esta barra durante el uso de ACIS en el Motorola.**

**Resultado:** se verificaron visualmente en el teléfono tanto la animación de carga de ubicación como la animación de la barra de progreso de las órdenes. Esta observación se sustenta en la prueba del responsable; no se adjuntó una grabación específica de la transición.

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

- Comprobar los permisos y el comportamiento de la ubicación cuando se deniega el acceso.
- Revisar la orientación de las fotos en distintas capturas.
- Revisar el comportamiento del teclado al completar formularios.
- Si el profesor lo solicita, preparar una grabación que muestre explícitamente la transición de la barra de progreso, ya observada por el responsable de las pruebas.
- Comprobar la adaptabilidad y navegación en distintas condiciones de uso.
- Revisar la evidencia de Trello y la distribución de tareas de ambos integrantes.
- Comprobar requisitos de entrega, README y estado de la rama compartida antes de presentar.
- Revisar y entregar el video explicativo de **3 minutos y 33 segundos**, ya preparado **sin voz, con subtítulos en español** y sin mostrar coordenadas reales.

El flujo completo de la OT 0001 **ya fue verificado**. La ubicación y la animación de carga de la OT 0002 **también fueron verificadas** y no deben seguir figurando como pendientes.

## 7. Relación con la rúbrica de Evaluación Parcial 2

| Criterio | Ponderación | Evidencia o estado |
|---|---:|---|
| IE 2.1.1 — Diseño y navegación | 15 % | Navegación funcional en las pantallas probadas; adaptabilidad y teclado por revisar. |
| IE 2.1.2 — Formularios validados | 15 % | Mensajes, bordes e íconos comprobados en creación, intervención y conformidad. |
| IE 2.2.1 — Validaciones desde la lógica | 10 % | Reglas centralizadas implementadas; comportamiento observado en las pruebas. |
| IE 2.2.2 — Animaciones funcionales | 10 % | Animación de carga del GPS y movimiento de la barra de progreso verificados visualmente por el responsable de las pruebas en Motorola. |
| IE 2.3.1 — MVVM y persistencia local | 15 % | Arquitectura presente; persistencia de OT cerrada y de ubicación de la OT 0002 comprobadas. |
| IE 2.3.2 — GitHub y Trello | 20 % | Commits en rama propia; evidencia de Trello pendiente de revisar. |
| IE 2.4.1 — Dos recursos nativos | 15 % | Cámara y ubicación del dispositivo probadas funcionalmente; escenarios de permisos no probados. |

Los resultados constituyen **evidencia parcial de pruebas**, no una nota ni una acreditación automática de todos los indicadores.

## 8. Trabajo colaborativo y control de versiones

**Repositorio:** `https://github.com/niquezadah/ACIS-APP`  
**Rama:** `claudio/validacion-cierre`

### 8.1. Commits verificados

**`2fdaa95`** — `feat: validar conformidad del cliente al cerrar ordenes`

Contribución técnica en `Order.kt`, `AcisViewModel.kt` y `AcisApp.kt` para la validación del cierre de órdenes.

**`543252f`** — `test: documentar pruebas manuales de ACIS en Motorola`

Creación de `verification/PRUEBAS_MANUALES_CLAUDIO.md` y primer registro de pruebas.

**`5def4fb`** — `test: verificar conformidad cierre y persistencia de OT 0001`

Actualización del documento con la prueba de cierre. Este es el commit ya subido a `claudio/validacion-cierre`; la presente actualización con GPS y animaciones requiere un nuevo commit antes de publicarse en GitHub.

La participación de ambos integrantes y la planificación real en Trello deben verificarse también. Este archivo no sustituye los registros de GitHub o Trello.

## 9. Estado general de verificación

**Resultado:** satisfactorio para el flujo principal de la OT 0001 y para el recurso de ubicación del dispositivo probado con la OT 0002.

Se comprobaron la creación y aceptación de órdenes; registro de llegada; validaciones de intervención; cámara y fotografía; registro de salida; conformidad del cliente; cierre definitivo y persistencia local después de reabrir ACIS. La OT 0002 permitió comprobar obtención, guardado y conservación de ubicación, además de la animación de carga correspondiente. El responsable confirmó visualmente el movimiento de la barra de progreso de las órdenes.

Quedan pendientes la revisión de determinados aspectos de **usabilidad**, escenarios de **permisos**, la evidencia de **Trello** y la revisión de entrega. La animación de la barra de progreso ya fue observada por el responsable; una grabación de esa transición puede añadirse como evidencia adicional si se requiere. Se preparó y aprobó un video de demostración de **3 minutos y 33 segundos**, **sin voz y con subtítulos en español**, evitando exponer coordenadas reales.

Las mejoras opcionales se evaluarán según la rúbrica, el caso ACIS y los acuerdos del equipo, evitando modificaciones innecesarias.

**Conclusión:** ACIS presenta su recorrido principal y los dos recursos nativos probados en un Motorola moto g55. Estas pruebas aportan evidencia favorable, pero no equivalen a una calificación ni permiten afirmar el cumplimiento íntegro de todos los criterios de la Evaluación Parcial 2.
