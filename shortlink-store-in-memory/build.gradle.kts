dependencies {
  implementation(project(":shortlink-lib"))
  implementation(libs.coroutines)

  testImplementation(kotlin("test"))
  testImplementation(libs.assertj.core)
  testImplementation(libs.coroutines.test)
  testImplementation(project(":shortlink-store-testing"))
}
