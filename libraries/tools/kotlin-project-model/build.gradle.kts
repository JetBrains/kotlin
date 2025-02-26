import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()

standardPublicJars()

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":kotlin-tooling-core"))
    testImplementation(kotlin("test-junit"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    kotlinOptions {
        languageVersion = "1.9"
        apiVersion = "1.9"
        freeCompilerArgs += listOf("-Xskip-prerelease-check", "-Xsuppress-version-warnings")
    }
}

tasks.named<Jar>("jar") {
    callGroovy("manifestAttributes", manifest, project)
}
