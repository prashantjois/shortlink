plugins {
  application
}

dependencies {
  implementation(project(":shortlink-lib"))
  implementation(project(":shortlink-store-in-memory"))
  implementation(project(":shortlink-store-jdbc"))
  implementation(project(":shortlink-store-mongodb"))
  implementation(project(":shortlink-store-dynamodb"))
  implementation(libs.awssdk.dynamodb.enhanced)
  implementation(libs.armeria)
  implementation(libs.hikari)
  implementation(libs.log4j.api)
  implementation(libs.log4j.core)
  implementation(libs.log4j.slf4j)
  implementation(libs.moshi)
  runtimeOnly(libs.jdbc.mysql)

  testImplementation(kotlin("test"))
  testImplementation(libs.assertj.core)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.armeria.junit5)
}

application {
  mainClass.set("shortlinkapp.AppKt")
}

val buildReact by tasks.registering(Exec::class) {
  workingDir = file("frontend") // Update with the path to your React app
  commandLine("npm", "run", "build")
}
