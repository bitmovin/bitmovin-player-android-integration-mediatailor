import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    alias(libs.plugins.jfrog.artifactory)
}

val groupId: String by rootProject
val versionName: String by rootProject
val artifactoryUser: String? by rootProject
val artifactoryPassword: String? by rootProject

if (artifactoryUser == null || artifactoryPassword == null) logger.warn(
    "Warning: Missing credentials to fetch and publish from Artifactory! " +
            "Please add them to ~/.gradle/gradle.properties"
)

android {
    val minSdkVersion: String by rootProject
    val compileSdkVersion: String by rootProject

    namespace = "com.bitmovin.player.integration.mediatailor"
    compileSdk = compileSdkVersion.toInt()

    defaultConfig {
        minSdk = minSdkVersion.toInt()

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
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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

project.afterEvaluate {
    publishing {
        publications {
            val projectGroupId = groupId
            create("release", MavenPublication::class) {
                from(components["release"])
                groupId = projectGroupId
                artifactId = project.name
                version = versionName
            }
        }
    }
}

artifactory {
    setContextUrl("https://bitmovin.jfrog.io/bitmovin")
    publish {
        repository {
            repoKey = if (version.toString().endsWith("SNAPSHOT")) {
                "libs-snapshot-local"
            } else {
                "libs-release-local"
            }
            username = artifactoryUser
            password = artifactoryPassword
        }
        defaults {
            publications(publishing.publications["release"])
            setPublishArtifacts(true)
            setPublishIvy(false) // Publish generated Ivy descriptor files to Artifactory (true by default)
        }
    }
}
