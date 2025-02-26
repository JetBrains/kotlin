plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

kotlin.sourceSets.configureEach {
    languageSettings.apiVersion = "1.9"
    languageSettings.languageVersion = "1.9"
}

dependencies {
    implementation(kotlinStdlib())
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}
