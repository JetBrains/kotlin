plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

dependencies {
    implementation(kotlinStdlib())
    implementation(commonDependency("com.google.code.gson:gson"))
    testImplementation(kotlinTest("junit"))
}
