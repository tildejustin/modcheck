plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.redlime"
version = "2.0.2"

repositories {
    mavenCentral()
    maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.jetbrains.intellij.java:java-gui-forms-rt:203.7148.30")
    implementation("com.formdev:flatlaf:3.3")
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
