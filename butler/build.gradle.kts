import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "me.javierfernandez"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = URI("https://repo.jenkins-ci.org/public")
    }
    maven {
        url = URI("https://repo.jenkins-ci.org/releases/")
    }
}

dependencies {
    implementation("org.jenkins-ci.main:jenkins-core:2.332")
    implementation("org.crashub:crash.connectors.ssh:1.3.2")
    implementation("org.crashub:crash.shell:1.3.2")
    implementation("org.crashub:crash.plugins:1.3.2")
    implementation("javax.xml.stream:stax-api:1.0-2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}
