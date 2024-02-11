import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
  implementation(project(":shortlink-lib"))
  implementation(libs.coroutines)
  implementation(libs.awssdk.dynamodb.enhanced)

  testImplementation(kotlin("test"))
  testImplementation(project(":shortlink-store-testing"))
  testImplementation(libs.assertj.core)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.testcontainers.core)
  testImplementation(libs.testcontainers.junit5)

  testRuntimeOnly(libs.junit5.engine)
}

