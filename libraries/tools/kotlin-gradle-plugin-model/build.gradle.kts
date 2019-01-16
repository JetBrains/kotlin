import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    kotlin("jvm")
    maven
}

standardPublicJars()
publish()

dependencies {
    compile(kotlinStdlib())
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.languageVersion = "1.2"
        kotlinOptions.apiVersion = "1.2"
        kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    named<Jar>("jar") {
        callGroovy("manifestAttributes", manifest, project)
    }
}