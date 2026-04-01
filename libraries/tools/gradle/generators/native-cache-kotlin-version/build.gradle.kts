plugins {
    kotlin("jvm")
    application
    id("project-tests-convention")
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
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJUnitPlatform()
    }
}