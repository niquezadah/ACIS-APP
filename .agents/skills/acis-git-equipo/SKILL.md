---
name: acis-git-equipo
description: "Proteger y coordinar cambios de ACIS entre Claudio y Nicolás en GitHub y Trello: verificar clon y rama correcta, hacer commits técnicos, pushes seguros, Pull Requests y respaldos entre el PC personal y el PC de Duoc."
---

# ACIS — Git y trabajo de equipo

**Repositorio oficial**: `https://github.com/niquezadah/ACIS-APP.git`; rama base `main`, rama de Claudio `claudio/validacion-cierre` (verificar en vivo). Consultar `../acis-continuidad/references/ESTADO_ACTUAL.md`.

## Antes de ejecutar o sugerir una operación de Git

1. Identificar **computador actual**, carpeta del proyecto, rama (`git status -sb`), `git remote -v`, archivos locales (`git diff --stat`) y cambios sin commit.
2. En PCs distintos puede haber clones distintos, incluso una carpeta `Android_16/ACIS-APP`. No inferir que la rama local exista. Nunca hacer `git reset --hard`, `git clean -fd`, `git push --force`, eliminar ramas o sobrescribir archivos sin aprobación y respaldo.
3. Si la rama remota de Claudio existe y no está local: después de comprobar cambios limpios, `git fetch origin`, `git switch --track origin/claudio/validacion-cierre`. Si hay cambios, preservarlos antes.
4. Commits solo de archivos relevantes, con mensajes técnicos concisos; revisar diff porque cambios de formato masivos (p. ej. 1206 inserciones/179 eliminaciones) complican el trabajo colaborativo.
5. No publicar directo a `main`. Subir la rama personal y crear PR tras compilar/probar y coordinar con Nicolás.
6. Para criterio IE 2.3.2 mantener **evidencia auténtica** de commits de ambos, PR/issues y planificación Trello. No inventar actividad.
7. Fecha de entrega: **confirmar** antes de cualquier push o PR; la pauta prohíbe modificar el repo después de la entrega y antes de evaluación.

## Pasos útiles (solo después de revisar estado)

```powershell
git status -sb
git remote -v
git branch -avv
git log -1 --oneline
```

No ejecutar automáticamente push, merge o PR si el usuario solo pidió orientación.
