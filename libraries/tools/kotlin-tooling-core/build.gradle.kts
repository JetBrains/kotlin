plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

dependencies {
    implementation(kotlinStdlib())
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}
