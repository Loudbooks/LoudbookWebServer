plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    application
}

group = "dev.loudbook"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.google.jimfs:jimfs:1.3.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(19)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "dev.loudbook.webserver.Main"
    }
}

application {
    mainClass.set("dev.loudbook.webserver.Main")
}