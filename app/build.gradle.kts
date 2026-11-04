plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.legacy.kapt)
}

// The web OAuth client is generated when Google is enabled in Firebase Authentication.
// Keep the email/password build usable with older Firebase configuration files too.
val firebaseConfig = groovy.json.JsonSlurper().parse(file("google-services.json")) as Map<*, *>
val firebaseAndroidClient = (firebaseConfig["client"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.firstOrNull {
    val clientInfo = it["client_info"] as? Map<*, *>
    (clientInfo?.get("android_client_info") as? Map<*, *>)?.get("package_name") == "com.example.sidehustle"
}
val firebaseWebClientId = (firebaseAndroidClient?.get("oauth_client") as? List<*>)
    ?.filterIsInstance<Map<*, *>>()?.firstOrNull { it["client_type"] == 3 }?.get("client_id") as? String ?: ""
val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
    .orElse(providers.environmentVariable("GOOGLE_WEB_CLIENT_ID")).getOrElse(firebaseWebClientId)

android {
    namespace = "com.example.sidehustle"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sidehustle"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "google_web_client_id", googleWebClientId)

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"https://sidehustle-production-f596.up.railway.app/\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lists and pull-to-refresh (shared loading patterns, see docs/LOADING_STATES.md)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)

    // Firebase BOM + Auth SDK — hashes passwords and hosts the user list for the demo
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)

    // Retrofit — REST client for the hosted API (issue #10)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.swiperefreshlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
