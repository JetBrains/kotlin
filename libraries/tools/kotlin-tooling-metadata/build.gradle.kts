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
    implementation(commonDependency("com.google.code.gson:gson"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}
