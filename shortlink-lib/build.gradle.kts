dependencies {
  implementation(libs.log4j.slf4j)
  implementation(libs.coroutines)

  testImplementation(kotlin("test"))
  testImplementation(libs.assertj.core)
  testImplementation(libs.coroutines.test)
}
