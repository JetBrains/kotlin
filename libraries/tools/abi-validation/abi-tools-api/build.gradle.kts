plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}
