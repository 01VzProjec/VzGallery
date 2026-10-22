plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vztechcore.vzgallery"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vztechcore.vzgallery"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
}

// 🔥 Força versões antigas compatíveis com compileSdk 35 e AGP 8.13
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.13.1")
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.activity:activity:1.9.0")
        force("androidx.activity:activity-ktx:1.9.0")
        force("androidx.activity:activity-compose:1.9.0")
        force("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
        force("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
        force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
        force("androidx.navigation:navigation-compose:2.7.7")
        force("androidx.transition:transition:1.5.1")
        force("androidx.navigationevent:navigationevent:1.0.0-alpha05")
        force("androidx.navigationevent:navigationevent-compose:1.0.0-alpha05")
    }
}

dependencies {
    // BOM do Compose — fixa em 2024.06 (estável com SDK 35)
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navegação
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    // 🔥 ExoPlayer (Media3) — versão antiga e estável
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")

    debugImplementation("androidx.compose.ui:ui-tooling")
}