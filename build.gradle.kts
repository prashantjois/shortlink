plugins {
  kotlin("jvm") version "1.9.0"
}

group = "ca.jois"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))
  testImplementation("org.assertj:assertj-core:3.11.1")
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(8)
}