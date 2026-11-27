import java.util.Properties
import java.net.URI
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
val acisApiBaseUrl = providers.gradleProperty("ACIS_API_BASE_URL").orElse("").get()
val cloudAccounts = providers.gradleProperty("ACIS_ACCOUNTS_URL").orNull
val cloudOrders = providers.gradleProperty("ACIS_FIELD_URL").orNull
require((cloudAccounts == null) == (cloudOrders == null)) {
 "Configura las dos direcciones del servidor o usa la configuración local."
}
listOfNotNull(cloudAccounts, cloudOrders).forEach { url ->
 val parsed = URI(url)
 require(parsed.scheme == "https" && parsed.host != null && parsed.rawUserInfo == null &&
  parsed.rawPath == "/" && parsed.rawQuery == null && parsed.rawFragment == null) {
  "Cada dirección de servidor debe usar HTTPS y terminar en /."
 }
}
val cloud = cloudAccounts != null
val signing = Properties().apply {
 val file = rootProject.file("firma/privada.properties")
 if(file.isFile) file.inputStream().use {load(it)}
}
android {
 namespace = "cl.acis.app"
 compileSdk { version = release(37) }
 defaultConfig {
  applicationId = "cl.acis.app"; minSdk = 26; targetSdk = 37
  versionCode = if (cloud) 3 else 2
  versionName = if (cloud) "1.0.1" else "1.0.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  buildConfigField("String", "FIELD_URL", "\""+(cloudOrders ?: "http://127.0.0.1:8080/")+"\"")
  buildConfigField("String", "ACCOUNTS_URL", "\""+(cloudAccounts ?: "http://127.0.0.1:8081/")+"\"")
  buildConfigField("String", "ACIS_API_BASE_URL", "\"${acisApiBaseUrl}\"")
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
 signingConfigs {
  if(signing.isNotEmpty()) create("entrega") {
   storeFile=rootProject.file("firma/acis-entrega.jks")
   storePassword=signing.getProperty("password")
   keyAlias="acis"; keyPassword=storePassword
  }
 }
 buildTypes {
  release { if(signing.isNotEmpty()) signingConfig=signingConfigs.getByName("entrega"); isMinifyEnabled=false }
  debug { enableUnitTestCoverage=true }
 }
 buildFeatures { compose = true; buildConfig = true }
 sourceSets.getByName("test").kotlin.srcDirs("src/androidTest/java/cl/acis/app/data","src/androidTest/java/cl/acis/app/viewmodel")
}
dependencies {
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("androidx.work:work-runtime-ktx:2.10.1")
 implementation(platform("androidx.compose:compose-bom:2026.02.01"))
 implementation("androidx.activity:activity-compose:1.8.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-text-google-fonts")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.core:core-ktx:1.10.1")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.navigation:navigation-compose:2.10.2")
 implementation("com.squareup.retrofit2:retrofit:2.11.0")
 implementation("com.squareup.retrofit2:converter-gson:2.11.0")
 testImplementation("junit:junit:4.13.2")
 testImplementation("androidx.test.ext:junit:1.3.0")
 testImplementation("org.json:json:20240303")
 testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
 testImplementation("org.robolectric:robolectric:4.16.1")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
tasks.withType<Test>().configureEach {
 doFirst {
  extensions.getByType(JacocoTaskExtension::class.java).apply {
   isIncludeNoLocationClasses=true
   includes=listOf("cl.acis.app.*")
  }
 }
}
