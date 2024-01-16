import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

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
    implementation(libs.armeria)
}

application {
    mainClass.set("shortlinkapp.AppKt")
}

kotlin {
    jvmToolchain(17)
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
