import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
}

group = "dev.kason.yousef"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    // kotlinx time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.0")
    implementation("io.ktor:ktor-server-netty:2.2.1")
    implementation("io.ktor:ktor-server-websockets:2.2.1")
    implementation("io.ktor:ktor-server-content-negotiation:2.2.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}