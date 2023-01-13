plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
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
    api(platform(project(":kotlin-gradle-plugins-bom")))
    compileOnly(kotlinStdlib())
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}