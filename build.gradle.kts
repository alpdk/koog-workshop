plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"

    // NEW compose plugin required for Kotlin 2.x+
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"

    // JetBrains Compose (UI toolkit)
    id("org.jetbrains.compose") version "1.6.10"

    application
}

group = "ai.koog"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)

    testImplementation(kotlin("test"))

    // Koog and your existing libs
    implementation(libs.koog.agents)
    implementation(libs.logback.classic)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ai.koog.workshop.dataset_report.UIKt")
}