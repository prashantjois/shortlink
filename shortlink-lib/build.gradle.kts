import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
  kotlin("jvm") version "1.9.0"
  alias(libs.plugins.ktfmt)
}

group = "ca.jois"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.log4j.api)
  implementation(libs.log4j.core)
  implementation(libs.log4j.slf4j)

  testImplementation(kotlin("test"))
  testImplementation(libs.assertj.core)
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(17)
}

ktfmt {
  // KotlinLang style - 4 space indentation - From https://kotlinlang.org/docs/coding-conventions.html
  kotlinLangStyle()
}

tasks.register<KtfmtFormatTask>("ktfmtPrecommit") {
  source = project.fileTree(rootDir)
  include("**/*.kt")
}
