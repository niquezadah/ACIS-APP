package cl.acis.app.connected

internal fun accountError(mode:String,email:String,password:String,name:String,recovery:String):String=when {
    !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim()) -> "Ingresa un correo válido."
    password.isEmpty() -> "Ingresa tu contraseña."
    mode!="Ingresar" && (password.length<10 || password.toByteArray().size>72) -> "Usa una contraseña de 10 a 72 bytes."
    mode=="Registrarme" && name.trim().length !in 2..120 -> "Ingresa tu nombre."
    mode=="Recuperar" && recovery.isBlank() -> "Ingresa tu clave de recuperación."
    else -> ""
}
