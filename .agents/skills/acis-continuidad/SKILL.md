---
name: acis-continuidad
description: "Retomar ACIS de Duoc UC exactamente desde el último punto sin pedir antecedentes repetidos; usar al empezar sesión, cambiar de computador/chat, crear o actualizar skills o registrar avances, decisiones, riesgos y tareas pendientes paso a paso."
---

# ACIS — Continuidad de trabajo

Leer `references/ESTADO_ACTUAL.md` y `references/HISTORIAL.md` antes de empezar. Si el usuario aporta evidencia más reciente, actualizar el registro (con su fecha y fuente) **sin borrar hechos anteriores**.

## Estilo de trabajo obligatorio

- Español claro, cercano y técnico solo cuando sea necesario. **Un paso por respuesta**, sin listas largas de instrucciones ni cambios no autorizados. Confirmación por pantalla/comando cuando aporte seguridad.
- No pedir de nuevo datos ya entregados. No añadir funcionalidades ajenas a la rúbrica y al acuerdo del equipo.
- Si el usuario solicita código para pegar, entregar lo necesario con ruta de archivo; mantener arquitectura y formato de código previo, y resaltar cambios.
- Separar: **verificado**, **reportado por usuario**, **pendiente de probar**, **hipótesis**. Nunca convertir un resumen antiguo en estado presente.
- Recordar preferencia por dispositivo físico **Motorola moto g55**, nunca emulador para pruebas de ACIS.

## En cada reanudación

1. Confirmar proyecto correcto (`ACIS-APP` cl.acis.app vs `Android_16` y ACIS plantilla), computador, raíz/branch y estado Git.
2. Leer fuentes de rúbrica (`acis-rubrica`), priorizar corrección que el usuario solicita.
3. Trabajar en orden pequeño: abrir archivo → cambio mínimo → compilar → probar → evidencia → commit.
4. Registrar cambios en `references/HISTORIAL.md` cuando el usuario indique actualizar las skills. El registro no sustituye `git log`.
5. Al cerrar sesión, resumir último paso realmente ejecutado y el **siguiente paso único**; señalar si falta commit/push.

**No actualizar automáticamente el repositorio remoto ni el archivo AGENTS.md sin revisar el proyecto y contar con aprobación.**
