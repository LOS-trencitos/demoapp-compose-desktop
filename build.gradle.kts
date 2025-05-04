import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.internal.de.undercouch.gradle.tasks.download.Download

val simplejavable_version = "v0.9.1"

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id ("de.undercouch.download") version "5.6.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jitpack.io")
}

tasks.register<Download>("downloadBLE") {
    src("https://github.com/simpleble/simpleble/releases/download/$simplejavable_version/simplejavable-$simplejavable_version.jar")
    dest(layout.projectDirectory)
}


dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // SimpleJavaBLE -
    implementation(files("simplejavable-$simplejavable_version.jar"))
    //implementation("com.github.simpleble:simpleble:main-SNAPSHOT")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "compose-junie-demo"
            packageVersion = "1.0.0"
        }
    }
}