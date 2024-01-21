dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("armeria", "1.26.4")
            version("assertj", "3.11.1")
            version("coroutines", "1.8.0-RC2")
            version("ktfmt", "0.16.0")
            version("log4j", "2.22.1")
            version("moshi", "1.14.0")

            plugin("ktfmt", "com.ncorti.ktfmt.gradle").versionRef("ktfmt")

            library("armeria", "com.linecorp.armeria", "armeria").versionRef("armeria")
            library("armeria-junit5", "com.linecorp.armeria", "armeria-junit5").versionRef("armeria")
            library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")
            library("coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines")
            library("coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("coroutines")
            library("log4j-api", "org.apache.logging.log4j", "log4j-api").versionRef("log4j")
            library("log4j-core", "org.apache.logging.log4j", "log4j-core").versionRef("log4j")
            library("log4j-slf4j", "org.apache.logging.log4j", "log4j-slf4j-impl").versionRef("log4j")
            library("moshi", "com.squareup.moshi", "moshi-kotlin").versionRef("moshi")
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
