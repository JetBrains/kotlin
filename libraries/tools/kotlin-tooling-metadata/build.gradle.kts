plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

publish()
sourcesJar()
javadocJar()

dependencies {
    implementation(kotlinStdlib())
    implementation("com.google.code.gson:gson:${rootProject.extra["versions.jar.gson"]}")
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}
