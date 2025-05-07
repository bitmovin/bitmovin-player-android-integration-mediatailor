import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bitmovin.player.integration.mediatailor"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

/** Auto opt-in for InternalBitmovinApi */
tasks.withType<KotlinJvmCompile>().all {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=com.bitmovin.player.core.internal.InternalBitmovinApi")
        freeCompilerArgs.add("-opt-in=com.bitmovin.player.core.internal.InternalPlayerApi")
        freeCompilerArgs.add("-opt-in=com.bitmovin.player.base.internal.InternalBitmovinApi")
        freeCompilerArgs.add("-opt-in=com.bitmovin.analytics.internal.InternalBitmovinApi")
    }
}

dependencies {
    compileOnly(libs.bitmovin.player)
    compileOnly(libs.bitmovin.player.base)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}