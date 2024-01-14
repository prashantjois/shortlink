import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shortlink-lib"))
    implementation(libs.coroutines)

    testImplementation(kotlin("test"))
    testImplementation(libs.assertj.core)
    testImplementation(libs.coroutines.test)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
