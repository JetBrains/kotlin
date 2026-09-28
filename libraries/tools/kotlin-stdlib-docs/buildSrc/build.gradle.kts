import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}
val dokka_version = project.property("dokka_version") as String

dependencies {
    implementation(libs.kotlin.dokka)
}

