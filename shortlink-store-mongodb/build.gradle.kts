import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
  implementation(project(":shortlink-lib"))
  implementation(libs.coroutines)
  implementation(libs.mongodb.driver)

  testImplementation(kotlin("test"))
  testImplementation(project(":shortlink-store-testing"))
  testImplementation(libs.assertj.core)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.junit5.api)
  testImplementation(libs.testcontainers.core)
  testImplementation(libs.testcontainers.junit5)
  testImplementation(libs.testcontainers.mongodb)
  testRuntimeOnly(libs.junit5.engine)
}

