import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "app.orchard.tv"
 compileSdk = 36
 defaultConfig {
  applicationId = "app.aethertv.cinema"
  minSdk = 23
  targetSdk = 34
  versionCode = 1
  versionName = "0.1.0"
 }
 buildFeatures { compose = true; buildConfig = true }
 buildTypes { release {
  isMinifyEnabled = true
  isShrinkResources = true
  proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  signingConfig = signingConfigs.getByName("debug")
 } }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 testOptions { unitTests.isIncludeAndroidResources = true }
 packaging { resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies {
 val composeBom = platform("androidx.compose:compose-bom:2025.11.01")
 implementation(composeBom)
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.animation:animation")
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.media3:media3-exoplayer:1.8.1")
 implementation("androidx.media3:media3-exoplayer-hls:1.8.1")
 implementation("androidx.media3:media3-exoplayer-dash:1.8.1")
 implementation("androidx.media3:media3-ui:1.8.1")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.17")
}

// Resolve exact runtime attribution data without adding a network service to the application.
tasks.register("writeRuntimeCoordinates") {
    val output = layout.buildDirectory.file("reports/runtime-coordinates.txt")
    outputs.file(output)
    doLast {
        val components = configurations.getByName("releaseRuntimeClasspath").incoming.resolutionResult.allComponents
            .mapNotNull { it.moduleVersion?.let { m -> "${m.group}:${m.name}:${m.version}" } }.distinct().sorted()
        output.get().asFile.apply { parentFile.mkdirs(); writeText(components.joinToString("\n",postfix="\n")) }
    }
}
