dependencies {
  implementation(project(":shortlink-lib"))
  implementation(libs.hikari)

  testImplementation(kotlin("test"))
  testImplementation(project(":shortlink-store-testing"))
  testImplementation(libs.assertj.core)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.hikari)
  testImplementation(libs.junit5.api)
  testImplementation(libs.junit5.engine)
  testImplementation(libs.junit5.params)
  testImplementation(libs.testcontainers.core)
  testImplementation(libs.testcontainers.junit5)
  testImplementation(libs.testcontainers.mariadb)
  testImplementation(libs.testcontainers.mysql)
  testImplementation(libs.testcontainers.postgresql)
  testRuntimeOnly(libs.jdbc.mysql)
  testRuntimeOnly(libs.jdbc.postgresql)
  testRuntimeOnly(libs.jdbc.mariadb)
}

