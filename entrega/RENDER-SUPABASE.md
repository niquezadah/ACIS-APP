# ACIS en Render y Supabase

El celular usa HTTPS para llamar a Cuentas y Órdenes en Render. Cada servicio guarda sus datos en su proyecto PostgreSQL de Supabase. Esta instalación parte con bases nuevas. Las fotos se guardan en la base de Órdenes y se mantienen si Render reinicia el servicio.

## Supabase

1. Crear los proyectos `acis-cuentas` y `acis-ordenes` en la misma región. En cada proyecto, dejar **Enable Data API** desactivado. El acceso a las tablas se hace desde los servicios Spring Boot.
2. En cada proyecto, abrir **Connect → Session pooler**. Anotar el host, el usuario `postgres.<referencia>` y la contraseña de PostgreSQL.
3. Ir a **Database → Settings**, habilitar **SSL Enforcement** y descargar el certificado CA de cada proyecto. Spring Boot crea las tablas al iniciar.

## Render

1. En **New → Blueprint**, conectar `niquezadah/ACIS-APP`, elegir `codex/tecnicos-y-locales` y cargar `render.yaml`. El archivo usa el Dockerfile de la raíz para compilar ambos servicios.
2. Ingresar los valores que pide Render. Para las URLs JDBC, reemplazar `HOST_DEL_POOLER` por el host de cada proyecto. Cuentas y Órdenes tienen sus propios datos de conexión.

   ```
   jdbc:postgresql://HOST_DEL_POOLER:5432/postgres?sslmode=verify-full&sslrootcert=/etc/secrets/supabase-ca.cer
   ```

   | Servicio | Variables privadas del panel |
   | --- | --- |
   | Cuentas | `ACIS_ACCOUNTS_DB`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `ACIS_ADMIN_EMAIL`, `ACIS_ADMIN_PASSWORD` |
   | Órdenes | `ACIS_DB_URL`, `ACIS_DB_USERNAME`, `ACIS_DB_PASSWORD`, `ACIS_ACCOUNTS_URL` |

   Elegir una contraseña nueva para Administración de al menos 10 caracteres y hasta 72 bytes. En `ACIS_ACCOUNTS_URL` va la URL pública HTTPS de Cuentas. Si Render todavía no la muestra al crear el Blueprint, usar `http://127.0.0.1:8081` como valor inicial y sustituirlo en Órdenes antes de probar la app.
3. En **Environment → Secret Files** de cada servicio, agregar el certificado de su base con el nombre `supabase-ca.cer`. Render lo deja disponible dentro del contenedor en `/etc/secrets/supabase-ca.cer`. Volver a desplegar con el archivo y las variables definitivas. El primer despliegue puede fallar mientras falte el certificado.
4. Abrir `/health` en cada URL pública. El código HTTP **200** confirma que ese servidor está levantado y llega a su base. Probar el ingreso y el listado de órdenes antes de compartir el APK.

## APK para el celular

Compilar con el JDK de Android Studio, la misma llave de firma de la entrega y las dos URLs HTTPS. Cada URL termina en una barra. Las claves de PostgreSQL y de Administración se ingresan en el panel privado de Render.

```powershell
.\gradlew.bat assembleRelease "-PACIS_ACCOUNTS_URL=https://HOST-CUENTAS/" "-PACIS_FIELD_URL=https://HOST-ORDENES/"
```

El APK de nube se genera en `app/build/outputs/apk/release/app-release.apk` como versión `1.0.1`. Puede actualizar el APK firmado de la entrega. Al abrirlo pide iniciar sesión con una cuenta del servidor nuevo. Para probarlo, desconectar el USB y registrar una orden ficticia desde otra conexión.

En el plan gratis conviene abrir ambos `/health` antes de una demostración. Si se habían suspendido por falta de uso, el primer arranque puede tomar cerca de un minuto.

[Blueprint de Render](https://render.com/docs/blueprint-spec) · [Archivos privados en Render](https://render.com/docs/configure-environment-variables) · [Conexión PostgreSQL](https://supabase.com/docs/guides/database/psql) · [Plan gratis de Render](https://render.com/docs/free)
