plugins {
  kotlin("jvm") version "1.9.0"
}

group = "ca.jois"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.slf4j:slf4j-api:1.7.25")

  implementation("org.apache.logging.log4j:log4j-api:2.22.1")
  implementation("org.apache.logging.log4j:log4j-core:2.22.1")
  implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.22.1")

  testImplementation(kotlin("test"))
  testImplementation("org.assertj:assertj-core:3.11.1")

}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(8)
}