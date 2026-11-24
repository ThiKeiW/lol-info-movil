plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.google.services)
}

// Web Client ID (tipo "Web application") del proveedor Google de Firebase; se define en
// gradle.properties como `google.webClientId=...`. No es secreto, pero se deja fuera del código.
val googleWebClientId: String = (project.findProperty("google.webClientId") as String?) ?: ""

android {
  namespace = "com.thikeiw.lolinfo"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.thikeiw.lolinfo"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "0.1.0"

    buildConfigField("String", "DD_URL", "\"https://ddragon.leagueoflegends.com/\"")
    buildConfigField("String", "CDRAGON_URL", "\"https://raw.communitydragon.org/\"")
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Coroutines
  implementation(libs.kotlinx.coroutines.android)

  // Serialization
  implementation(libs.kotlinx.serialization.json)

  // Dependency injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

  // Networking
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.kotlinx.serialization)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)

  // Local persistence
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.datastore.preferences)

  // Paging
  implementation(libs.androidx.paging.runtime)
  implementation(libs.androidx.paging.compose)

  // Images
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  // Firebase (Auth + Firestore)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)

  // Google Sign-In via Credential Manager
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
}
