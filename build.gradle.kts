plugins {
    kotlin("jvm") version "2.5.0-RC"
    kotlin("plugin.serialization") version "2.5.0-RC"
}

group = "com.redlime"
version = "3.2.0"

repositories {
    mavenCentral()
    maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
    implementation("com.jetbrains.intellij.java:java-gui-forms-rt:263.7307.259")
    implementation("com.formdev:flatlaf:3.8")
    implementation("io.github.z4kn4fein:semver:3.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Main-Class" to "com.pistacium.modcheck.ModCheck",
            "Implementation-Version" to version
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
}
