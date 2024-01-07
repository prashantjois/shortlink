plugins {
  kotlin("jvm") version "1.9.0"
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
  jvmToolchain(8)
}