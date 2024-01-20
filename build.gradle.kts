plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.redlime"
version = "2.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.intellij:forms_rt:7.0.3")
    implementation("com.formdev:flatlaf:3.3")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "com.pistacium.modcheck.ModCheck"
}
