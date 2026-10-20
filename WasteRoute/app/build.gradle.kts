import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val envProperties = Properties()
val envFile = project.file(".env").takeIf { it.exists() }
    ?: rootProject.file(".env").takeIf { it.exists() }
    ?: rootProject.file("../.env").takeIf { it.exists() }

if (envFile != null && envFile.exists()) {
    FileInputStream(envFile).use { envProperties.load(it) }
}

val keystoreProps = Properties()
rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f ->
    FileInputStream(f).use { keystoreProps.load(it) }
}

android {
    namespace = "com.wasteroute.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wasteroute.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val googleOAuthClientId = envProperties.getProperty("GOOGLE_OAUTH_CLIENT_ID", "")
        val apiBaseUrl = envProperties.getProperty("API_BASE_URL", "")
        val municipalPortalUrl = envProperties.getProperty("MUNICIPAL_PORTAL_URL", "")
        val osmTileUrl = envProperties.getProperty("OSM_TILE_URL", "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
        val campusLat = envProperties.getProperty("CAMPUS_LAT", "12.8230")
        val campusLng = envProperties.getProperty("CAMPUS_LNG", "80.0440")
        val campusName = envProperties.getProperty("CAMPUS_NAME", "SRM IST Kattankulathur (SRM KTR)")

        buildConfigField("String", "GOOGLE_OAUTH_CLIENT_ID", "\"$googleOAuthClientId\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "MUNICIPAL_PORTAL_URL", "\"$municipalPortalUrl\"")
        buildConfigField("String", "OSM_TILE_URL", "\"$osmTileUrl\"")
        buildConfigField("String", "CAMPUS_LAT", "\"$campusLat\"")
        buildConfigField("String", "CAMPUS_LNG", "\"$campusLng\"")
        buildConfigField("String", "CAMPUS_NAME", "\"$campusName\"")
    }

    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf("src/main/java"))
            kotlin.setSrcDirs(listOf("src/main/java"))
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    exclude("**/dataconnect/generated/**")
}



dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Firebase Authentication & Analytics & Firestore Database
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)

    // Google Sign-In & Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    // Fused location (auto-detect citizen location, worker/truck live GPS)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // QR code generation (worker) and scanning (citizen)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    debugImplementation(libs.androidx.ui.tooling)
}
