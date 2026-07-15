plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
    id("project-tests-convention")
    id("test-inputs-check-v2")
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
