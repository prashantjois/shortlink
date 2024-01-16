dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("log4j", "2.22.1")
            version("assertj", "3.11.1")
            version("ktfmt", "0.16.0")
            version("coroutines", "1.8.0-RC2")
            version("armeria", "1.26.4")

            plugin("ktfmt", "com.ncorti.ktfmt.gradle").versionRef("ktfmt")

            library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")
            library("log4j-api", "org.apache.logging.log4j", "log4j-api").versionRef("log4j")
            library("log4j-core", "org.apache.logging.log4j", "log4j-core").versionRef("log4j")
            library("log4j-slf4j", "org.apache.logging.log4j", "log4j-slf4j-impl").versionRef("log4j")
            library("coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")
            library("coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("coroutines")
            library("armeria", "com.linecorp.armeria", "armeria").versionRef("armeria")
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
include("shortlink-app")
