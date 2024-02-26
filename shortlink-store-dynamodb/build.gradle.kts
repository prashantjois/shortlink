dependencies {
  implementation(project(":shortlink-lib"))
  implementation(libs.coroutines)
  implementation(libs.awssdk.dynamodb.enhanced)
  implementation(libs.moshi)

  testImplementation(kotlin("test"))
  testImplementation(project(":shortlink-store-testing"))
  testImplementation(libs.assertj.core)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.testcontainers.core)
  testImplementation(libs.testcontainers.junit5)

  testRuntimeOnly(libs.junit5.engine)
}

