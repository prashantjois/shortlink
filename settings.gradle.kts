dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("log4j", "2.22.1")
      version("assertj", "3.11.1")

      library("log4j-api", "org.apache.logging.log4j", "log4j-api").versionRef("log4j")
      library("log4j-core", "org.apache.logging.log4j", "log4j-core").versionRef("log4j")
      library("log4j-slf4j", "org.apache.logging.log4j", "log4j-slf4j-impl").versionRef("log4j")

      library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")
    }
  }
}

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "shortlink"

include("shortlink-lib")