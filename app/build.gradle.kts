import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.charset.Charset

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")

    id("com.apollographql.apollo3") version "3.8.2"
    id("io.sentry.android.gradle") version "4.1.1"

    kotlin("plugin.serialization") version "1.9.22"
}

fun getLocalIPv4(): List<String> {
    return NetworkInterface.getNetworkInterfaces()
        .toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .flatMap { networkInterface ->
            networkInterface.inetAddresses
                .toList()
                .filter { !it.isLoopbackAddress && it is Inet4Address }
                .map { println(it.hostAddress); it.hostAddress }
        }
}

android {
    namespace = "net.einsa.lotta"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.einsa.lotta"
        minSdk = 28
        targetSdk = 34
        versionCode = 14
        versionName = "0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "API_HOST", "\"core.lotta.schule\"")
        buildConfigField("Boolean", "USE_SECURE_CONNECTION", "true")
    }

    flavorDimensions += "endpoint"
    productFlavors {
        create("production") {
            dimension = "endpoint"
            isDefault = true
        }
        create("staging") {
            dimension = "endpoint"
            applicationIdSuffix = ".staging"

            buildConfigField("String", "API_HOST", "\"core.staging.lotta.schule\"")
            buildConfigField("Boolean", "USE_SECURE_CONNECTION", "true")
        }

        create("local") {
            dimension = "endpoint"
            applicationIdSuffix = ".local"

            buildConfigField(
                "String",
                "API_HOST",
                "\"${getLocalIPv4().firstOrNull() ?: "localhost"}:4000\""
            )
            buildConfigField("Boolean", "USE_SECURE_CONNECTION", "false")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "DebugProbesKt.bin"
                )
            )
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")
    implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite:3.8.2")
    implementation("com.google.firebase:firebase-messaging-ktx:24.0.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

apollo {
    service("service") {
        packageName.set("net.einsa.lotta")

        mapScalarToKotlinString("DateTime")
    }
}

sentry {
    org.set("lotta")
    projectName.set("android")

    authToken.set(System.getenv("SENTRY_AUTH_TOKEN"))

    includeProguardMapping.set(true)
    autoUploadProguardMapping.set(true)
    autoUploadNativeSymbols.set(true)

    includeNativeSources.set(true)
    includeSourceContext.set(true)

    tracingInstrumentation {
        enabled.set(true)
        features.set(setOf(
            InstrumentationFeature.DATABASE,
            InstrumentationFeature.FILE_IO,
            InstrumentationFeature.OKHTTP,
            InstrumentationFeature.COMPOSE
        ))

        logcat {
            enabled.set(true)

            // Specifies a minimum log level for the logcat breadcrumb logging.
            // Defaults to LogcatLevel.WARNING.
            minLevel.set(LogcatLevel.WARNING)
        }

        autoInstallation {
            enabled.set(true)
        }

        includeDependenciesReport.set(true)

        telemetry.set(true)
    }
}

task("bumpBuildNumber") {
    doLast {
        buildFile.readText(Charset.defaultCharset())
            .replace("(versionCode\\s*=\\s*)(\\d+)".toRegex()) { matchResult ->
                val newVersionCode = matchResult.groupValues[2].toInt() + 1
                return@replace "${matchResult.groupValues[1]}${newVersionCode}"
            }
            .let { updatedText ->
                buildFile.writeText(updatedText, Charset.defaultCharset())
            }
    }
}