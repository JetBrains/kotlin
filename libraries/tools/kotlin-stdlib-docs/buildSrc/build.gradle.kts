import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}
val dokka_version: String by project

dependencies {
    implementation(libs.kotlin.dokka)
}

