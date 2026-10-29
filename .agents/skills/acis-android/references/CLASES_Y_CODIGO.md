# Apuntes del profesor — ACIS / DSY1105

**Origen:** fotografías de la pantalla del profesor y transcripciones en conversación, más informe de transferencia. No afirmar que las transcripciones son literales: algunas palabras fueron captadas incompletas.

- Kotlin y Compose: `@Composable`, `Modifier`, `Row`, `Column`, `Box`, `Text`, `Button`, `OutlinedTextField`, `Card`, `Scaffold`, `TopAppBar`, `FloatingActionButton`, Material 3; `@Preview` cuando sea posible.
- Navegación: `rememberNavController()`, `NavHost`, `composable`, `navigate`, pila back stack y `popBackStack()` para volver (evitar `navigate("home")` como regreso). Botón de atrás en `TopAppBar` -> `navigationIcon` + `IconButton` + `Icon`/`ImageVector`; `contentDescription = "Volver"`.
- El profesor demostró primero `println("holi")` en `onClick` y luego una navegación real. `Icons.Default.ArrowBackIosNew` apareció en foto; explicó Material Icons Core / Extended. No suponer automáticamente que ambas dependencias ya están instaladas.
- `Scaffold` proporciona ranura `floatingActionButton` para navegar a pantalla de agregar; icono Add y forma/color Material 3. FAB solo cuando cumple su función, sin imponerlo a todos los formularios.
- Estado/arquitectura: `ViewModel`, `MutableStateFlow` privado y `StateFlow` público, `copy()` para actualizar estado, validación de campos fuera de UI, Compose reacciona al estado. Uso de snackbar, diálogos, animaciones de progreso/carga.
- Persistencia abordada en curso: Room (`Entity`, `DAO`, `Database`) y SQLite; en el código actual ACIS usa SQLite con repositorio, no Room. Cambiar solo si rubric/requerimiento o mejora aprobada lo justifican.
- Recursos nativos abordados: cámara, galería, GPS/ubicación, permisos, Activity Result. No asumir que todos requieren el mismo permiso.

## Fuentes educativas citadas en clases

- https://github.com/profealejandroduoc/layoutsKotlin2026
- https://github.com/profealejandroduoc/sintaxis_kotlin_2026
- https://developer.android.com/develop/ui/compose/navigation
- https://developer.android.com/compose
- https://developer.android.com/training/data-storage/room
- https://developer.android.com/training/permissions/requesting

**Nota:** priorizar original del profesor y del proyecto cuando difiera de este resumen.
