dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("armeria", "1.26.4")
      version("assertj", "3.11.1")
      version("coroutines", "1.8.0-RC2")
      version("hikari", "5.1.0")
      version("jdbc-mariadb", "3.1.0")
      version("jdbc-mysql", "8.3.0")
      version("jdbc-postgresql", "42.7.1")
      version("junit", "5.10.1")
      version("log4j", "2.22.1")
      version("mongodb", "4.11.1")
      version("moshi", "1.14.0")
      version("testcontainers", "1.19.4")
      version("awssdk", "2.20.56")

      library("armeria", "com.linecorp.armeria", "armeria").versionRef("armeria")
      library("armeria-junit5", "com.linecorp.armeria", "armeria-junit5").versionRef("armeria")
      library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")
      library(
        "coroutines",
        "org.jetbrains.kotlinx",
        "kotlinx-coroutines-core",
      ).versionRef("coroutines")
      library(
        "coroutines-test",
        "org.jetbrains.kotlinx",
        "kotlinx-coroutines-test",
      ).versionRef("coroutines")
      library("hikari", "com.zaxxer", "HikariCP").versionRef("hikari")
      library("jdbc-mariadb", "org.mariadb.jdbc", "mariadb-java-client").versionRef("jdbc-mariadb")
      library("jdbc-mysql", "com.mysql", "mysql-connector-j").versionRef("jdbc-mysql")
      library("jdbc-postgresql", "org.postgresql", "postgresql").versionRef("jdbc-postgresql")
      library("junit5-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
      library("junit5-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
      library("junit5-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit")
      library("log4j-api", "org.apache.logging.log4j", "log4j-api").versionRef("log4j")
      library("log4j-core", "org.apache.logging.log4j", "log4j-core").versionRef("log4j")
      library("log4j-slf4j", "org.apache.logging.log4j", "log4j-slf4j-impl").versionRef("log4j")
      library("mongodb-driver", "org.mongodb", "mongodb-driver-sync").versionRef("mongodb")
      library("moshi", "com.squareup.moshi", "moshi-kotlin").versionRef("moshi")
      library(
        "testcontainers-core",
        "org.testcontainers",
        "testcontainers",
      ).versionRef("testcontainers")
      library(
        "testcontainers-junit5",
        "org.testcontainers",
        "junit-jupiter",
      ).versionRef("testcontainers")
      library(
        "testcontainers-mariadb",
        "org.testcontainers",
        "mariadb",
      ).versionRef("testcontainers")
      library(
        "testcontainers-mongodb",
        "org.testcontainers",
        "mongodb",
      ).versionRef("testcontainers")
      library("testcontainers-mysql", "org.testcontainers", "mysql").versionRef("testcontainers")
      library(
        "testcontainers-postgresql",
        "org.testcontainers",
        "postgresql",
      ).versionRef("testcontainers")
      library("awssdk-dynamodb-enhanced", "software.amazon.awssdk", "dynamodb-enhanced").versionRef(
        "awssdk",
      )
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
include("shortlink-store-in-memory")
include("shortlink-store-jdbc")
include("shortlink-store-mongodb")
include("shortlink-store-dynamodb")
include("shortlink-store-testing")
include("shortlink-app")
