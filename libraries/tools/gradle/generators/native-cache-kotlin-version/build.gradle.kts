plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
    id("test-inputs-check")
}

dependencies {
    implementation(libs.kotlinpoet)
    implementation(project(":generators"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
    testImplementation(kotlinTest("junit5"))
}

application {
    mainClass.set("org.jetbrains.kotlin.gradle.generators.version.MainKt")
}

projectTests {
    testTask()
}
