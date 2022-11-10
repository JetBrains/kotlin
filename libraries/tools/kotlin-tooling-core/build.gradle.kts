plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

kotlin.sourceSets.configureEach {
    languageSettings.apiVersion = "1.4"
    languageSettings.languageVersion = "1.4"
}

dependencies {
    compileOnly(kotlinStdlib())
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}
