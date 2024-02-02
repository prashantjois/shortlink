import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.9.21"
    alias(libs.plugins.ktfmt)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shortlink-lib"))
    implementation(project(":shortlink-in-memory-store"))
    implementation(project(":shortlink-jdbc-store"))
    implementation(libs.armeria)
    implementation(libs.moshi)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j)
    implementation(libs.hikari)
    runtimeOnly(libs.jdbc.mysql)

    testImplementation(kotlin("test"))
    testImplementation(libs.assertj.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.armeria.junit5)
}

application {
    mainClass.set("shortlinkapp.AppKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

tasks.test {
    useJUnitPlatform()
}

ktfmt {
    // KotlinLang style - 4 space indentation - From https://kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
}

tasks.register<KtfmtFormatTask>("ktfmtPrecommit") {
    source = project.fileTree(rootDir)
    include("**/*.kt")
}