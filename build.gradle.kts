plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "com.redlime"
version = "3.1.0"

repositories {
    mavenCentral()
    maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.jetbrains.intellij.java:java-gui-forms-rt:203.7148.30")
    implementation("com.formdev:flatlaf:3.3")
    implementation("io.github.z4kn4fein:semver:1.4.2")
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
