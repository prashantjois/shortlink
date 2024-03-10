import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.21"
}

allprojects {
  group = "ca.jois"
  version = "0.1"

  repositories { mavenCentral() }

  apply {
    plugin("kotlin")
  }

  kotlin { jvmToolchain(17) }

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions { freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers" }
  }

  tasks.test { useJUnitPlatform() }
}
