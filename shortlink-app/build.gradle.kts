plugins {
    kotlin("jvm") version "1.9.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shortlink-lib"))
}

application {
    mainClass.set("shortlinkapp.AppKt")
}

kotlin {
    jvmToolchain(17)
}
