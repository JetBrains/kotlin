plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    // remove stdlib dependency from api artifact in order not to affect the dependencies of the user project
    compileOnly(kotlinStdlib())

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}
