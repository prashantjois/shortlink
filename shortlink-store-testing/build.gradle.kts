import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    implementation(project(":shortlink-lib"))
    implementation(libs.coroutines)

    implementation(libs.assertj.core)
    implementation(libs.junit5.api)
    runtimeOnly(libs.junit5.engine)
    implementation(libs.coroutines.test)
}

