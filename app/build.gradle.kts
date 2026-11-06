import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.charset.Charset

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")

    id("com.apollographql.apollo") version "4.4.4"
    id("io.sentry.android.gradle") version "6.14.0"

    kotlin("plugin.serialization") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
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
    compileSdk = 37

    defaultConfig {
        applicationId = "net.einsa.schule.lotta"
        minSdk = 28
        targetSdk = 34
        versionCode = 26
        versionName = "0.13"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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

androidComponents {
    beforeVariants { variantBuilder ->
        (variantBuilder as? com.android.build.api.variant.HasUnitTestBuilder)?.enableUnitTest = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.fragment:fragment:1.9.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.security:security-crypto:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.auth0:java-jwt:4.6.2")
    implementation("com.apollographql.apollo:apollo-runtime:4.4.4")
    implementation("com.apollographql.apollo:apollo-normalized-cache-sqlite:4.4.4")
    implementation("com.apollographql.apollo:apollo-normalized-cache:4.4.4")
    implementation("com.google.firebase:firebase-messaging:26.0.1")
    implementation("androidx.compose.material3.adaptive:adaptive-android:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-test-android:1.12.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.16")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
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
        features.set(
            setOf(
                InstrumentationFeature.DATABASE,
                InstrumentationFeature.FILE_IO,
                InstrumentationFeature.OKHTTP,
                InstrumentationFeature.COMPOSE
            )
        )

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

tasks.register("bumpBuildNumber") {
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

// Usage: ./gradlew bumpVersion -Pbump=major|minor|patch
// Bumps versionName (semver; a missing patch part counts as 0) and increments versionCode.
tasks.register("bumpVersion") {
    val bump = providers.gradleProperty("bump")
    doLast {
        val part = bump.orNull ?: throw GradleException("Missing -Pbump=major|minor|patch")
        val versionNameRegex = "(versionName\\s*=\\s*\")([^\"]+)(\")".toRegex()
        val text = buildFile.readText(Charset.defaultCharset())
        val current = versionNameRegex.find(text)?.groupValues?.get(2)
            ?: throw GradleException("versionName not found in ${buildFile.name}")
        val (major, minor, patch) = current.split(".").map { it.toInt() }
            .let { it + List(3 - it.size) { 0 } }
        val next = when (part) {
            "major" -> "${major + 1}.0.0"
            "minor" -> "$major.${minor + 1}.0"
            "patch" -> "$major.$minor.${patch + 1}"
            else -> throw GradleException("Invalid bump '$part', expected major, minor or patch")
        }
        text
            .replace(versionNameRegex) { "${it.groupValues[1]}$next${it.groupValues[3]}" }
            .replace("(versionCode\\s*=\\s*)(\\d+)".toRegex()) {
                "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
            }
            .let { buildFile.writeText(it, Charset.defaultCharset()) }
        println("VERSION_NAME=$next")
    }
}

tasks.matching {
    it.name.startsWith("generateSentryBundleId") ||
        it.name.startsWith("sentryCollectSources") ||
        it.name.startsWith("sentryUploadSourceBundle")
}.configureEach {
    dependsOn(tasks.matching { it.name.contains("ApolloSources") })
}

