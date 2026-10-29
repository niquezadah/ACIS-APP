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

Verificar el funcionamiento de la aplicación móvil ACIS en un dispositivo Android físico, comprobando la navegación, creación de órdenes de trabajo, validaciones de formularios, cambios de estado y persistencia local.

Las pruebas se realizan considerando los requisitos del caso ACIS, los criterios de la rúbrica de la Evaluación Parcial 2 y las instrucciones entregadas por el profesor.

Se busca identificar tanto las funcionalidades que operan correctamente como los problemas que podrían afectar la evaluación.

## 2. Pruebas realizadas

| Prueba | Resultado |
|---|---|
| Compilación mediante Gradle | Correcto — BUILD SUCCESSFUL |
| Ejecución en Motorola moto g55 | Correcto |
| Navegación entre pantallas probadas | Correcto |
| Selección de perfiles Jefatura y Técnico | Correcto |
| Validación de campos obligatorios al crear órdenes | Correcto |
| Mensajes e íconos de error en formulario de creación | Correcto |
| Creación de orden OT 0001 | Correcto |
| Cambio de estado Asignada a Aceptada | Correcto |
| Cambio de estado Aceptada a En atención | Correcto |
| Registro de fecha y hora de llegada | Correcto |
| Conservación de la orden al reabrir ACIS | Correcto |
| Validación de diagnóstico obligatorio | Correcto |
| Validación de trabajo realizado obligatorio | Correcto |
| Validación de fotografía obligatoria | Correcto |

**Resultado general:** las funcionalidades indicadas como correctas fueron verificadas durante las pruebas realizadas. Todavía no se ha completado el flujo de cierre de órdenes.

## 3. Validaciones comprobadas

### 3.1. Creación de órdenes de trabajo

Se intentó crear una orden dejando los campos obligatorios vacíos.

La aplicación mostró mensajes específicos, bordes rojos e íconos de advertencia.

Se verificaron las siguientes reglas:

- Nombre del local obligatorio.
- Dirección de la visita obligatoria.
- Identificación del equipo obligatoria.
- Técnico asignado obligatorio.
- Motivo de la visita con un mínimo de 5 caracteres.

Posteriormente se completó el formulario con información ficticia y se creó correctamente la orden OT 0001.

### 3.2. Registro de intervención

Se intentó finalizar una intervención sin completar los datos requeridos.

La aplicación detectó:

- Diagnóstico incompleto.
- Descripción del trabajo realizado incompleta.
- Ausencia de fotografía como evidencia.

Se observaron mensajes de validación para los campos obligatorios.

Los materiales y la ubicación permanecieron como datos opcionales.

### 3.3. Validación de conformidad del cliente

Se incorporó una regla de validación para comprobar el nombre del cliente y su aceptación explícita antes de cerrar una orden.

La validación está implementada en la capa lógica y conectada al `ViewModel`.

**Estado:** implementación compilada correctamente. Pendiente de verificar el comportamiento completo en el teléfono.

## 4. Persistencia local y estados

Se creó la orden OT 0001 utilizando el perfil Jefatura.

Posteriormente se realizaron las siguientes acciones:

1. Creación de la orden.
2. Cambio al perfil Técnico.
3. Aceptación de la orden.
4. Registro de llegada.
5. Inicio de la atención.
6. Cierre y reapertura de la aplicación para comprobar la conservación de datos.

La orden conservó sus datos y el estado **En atención**.

La llegada quedó registrada el 29/10/2026 a las 10:25.

Esta prueba proporciona evidencia de persistencia local para los datos examinados. Todavía falta comprobar el almacenamiento de una intervención finalizada y una orden cerrada.

## 5. Observaciones y mejoras pendientes

Las siguientes observaciones se registran como resultado de las pruebas realizadas por el equipo.

**No todas representan incumplimientos de la rúbrica.** Se distingue entre requisitos expresos, problemas que afectan el funcionamiento, mejoras opcionales y dificultades del entorno de desarrollo.

| Observación | Clasificación | Justificación |
|---|---|---|
| El buscador no encuentra `OT 0001` | Mejora opcional | La rúbrica no exige explícitamente este formato de búsqueda. |
| El teclado tapa campos del formulario | Corrección necesaria si impide ingresar datos | Puede afectar la usabilidad y el criterio IE 2.1.1. |
| Algunas fotografías aparecen giradas | Corrección recomendable | Puede afectar la visualización de evidencias y la integración funcional de la cámara. |
| Permitir hasta 10 fotografías por orden | Mejora acordada por el equipo | La rúbrica no especifica esta cantidad, pero el equipo solicitó implementarla. |
| Error de firma digital al instalar desde diferentes computadores | Problema del entorno | No representa por sí mismo un incumplimiento funcional de ACIS. |

### 5.1. Criterio para futuras modificaciones

Se priorizarán:

1. Los requisitos expresamente establecidos en la rúbrica y en las instrucciones de la evaluación.
2. Las correcciones necesarias para garantizar el funcionamiento y la usabilidad de ACIS.
3. Las funcionalidades acordadas por ambos integrantes del equipo.
4. Las mejoras opcionales que no comprometan la estabilidad del proyecto.

Una mejora no se considerará obligatoria únicamente por haber sido detectada durante las pruebas.

Sin embargo, un problema puede necesitar corrección aunque no esté mencionado literalmente en la rúbrica, cuando impida cumplir un criterio general.

**Estado:** observaciones documentadas. Las correcciones todavía no se consideran implementadas.

## 6. Pruebas pendientes

Antes de considerar completado el recorrido funcional, todavía debemos:

- Comprobar la captura y visualización de fotografías.
- Verificar la orientación de las fotografías.
- Comprobar la obtención de ubicación GPS.
- Completar el formulario de intervención.
- Finalizar la visita y registrar la salida.
- Validar el nombre y la conformidad del cliente.
- Cerrar definitivamente una orden de trabajo.
- Verificar la persistencia de una orden cerrada.
- Revisar los problemas de usabilidad detectados por el equipo.

Las pruebas deben continuar preferentemente en un dispositivo Android físico.

## 7. Relación con la rúbrica de Evaluación Parcial 2

| Criterio | Ponderación | Evidencia o estado |
|---|---:|---|
| IE 2.1.1 — Diseño y navegación | 15 % | Navegación funcional en pantallas probadas; adaptabilidad todavía no verificada completamente |
| IE 2.1.2 — Formularios validados | 15 % | Mensajes e íconos comprobados en creación de órdenes |
| IE 2.2.1 — Validaciones desde la lógica | 10 % | Reglas centralizadas implementadas y validaciones observadas |
| IE 2.2.2 — Animaciones funcionales | 10 % | Pendiente de verificación específica |
| IE 2.3.1 — MVVM y persistencia local | 15 % | Arquitectura implementada y conservación de orden comprobada |
| IE 2.3.2 — GitHub y Trello | 20 % | Contribución en rama propia registrada; evidencia de Trello pendiente |
| IE 2.4.1 — Dos recursos nativos | 15 % | Cámara y GPS integrados en el proyecto; pruebas funcionales pendientes |

Los resultados documentados representan únicamente las pruebas y verificaciones realizadas hasta la fecha.

No constituyen una calificación ni acreditan el cumplimiento completo de cada criterio.

## 8. Trabajo colaborativo y control de versiones

El proyecto se desarrolla colaborativamente mediante el repositorio:

`https://github.com/niquezadah/ACIS-APP`

Las contribuciones técnicas deben realizarse en ramas de trabajo identificables, utilizando mensajes de commit descriptivos.

La rama `claudio/validacion-cierre` contiene la contribución relacionada con las validaciones para cerrar órdenes de trabajo.

El historial de GitHub y la planificación en Trello deberán mantenerse como evidencia de colaboración real entre los integrantes.

## 9. Estado general de verificación

**Resultado:** pruebas parciales satisfactorias.

Se verificaron la compilación, la ejecución en Motorola, la creación de órdenes, las validaciones de campos, las transiciones de estado examinadas y la conservación de los datos registrados.

Se mantienen pendientes las pruebas de cámara, GPS, cierre de órdenes y conformidad del cliente.

Las observaciones opcionales no se consideran automáticamente requisitos obligatorios. Su implementación deberá justificarse según la rúbrica, el caso ACIS y los acuerdos del equipo.

**Conclusión:** el proyecto presenta avances funcionales comprobados, pero todavía requiere completar las pruebas pendientes antes de declarar que cumple íntegramente la Evaluación Parcial 2.