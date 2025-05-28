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
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    compileOnly(libs.bitmovin.player)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.bitmovin.player)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest)
    testImplementation(libs.mockk.core)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.strikt.core)
    testImplementation(libs.strikt.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.withType<KotlinJvmCompile>().all {
    if (this.name.contains("Test")) return@all

    compilerOptions.allWarningsAsErrors.set(true)
    compilerOptions {
        // Enable explicit API for production code
        freeCompilerArgs.add("-Xexplicit-api=strict")
    }
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
