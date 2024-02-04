import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.21"
  alias(libs.plugins.ktfmt)
}

allprojects {
  group = "ca.jois"
  version = "0.1"

  repositories { mavenCentral() }

  apply {
    plugin("kotlin")
    plugin("com.ncorti.ktfmt.gradle")
  }

  kotlin { jvmToolchain(17) }

  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions { freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers" }
  }

  tasks.test { useJUnitPlatform() }

  ktfmt {
    // KotlinLang style - 4 space indentation - From
    // https://kotlinlang.org/docs/coding-conventions.html
    kotlinLangStyle()
  }

  tasks.register<KtfmtFormatTask>("ktfmtPrecommit") {
    source = project.fileTree(rootDir)
    include("**/*.kt")
  }
}
