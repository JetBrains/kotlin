plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

projectTests {
    testTask {
        // Disable KONAN_DATA_DIR env variable for all integration tests
        // because we are using `konan.data.dir` gradle property instead
        environment.remove("KONAN_DATA_DIR")

        dependsOnKotlinGradlePluginInstall()

        if (project.kotlinBuildProperties.isKotlinNativeEnabled.get()) {
            // Build full Kotlin Native bundle
            dependsOn(":kotlin-native:install")
        }

        systemProperty("kotlinVersion", kotlinBuildProperties.kotlinVersion.get())
    }
}

dependencies {
    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(project(":kotlin-compiler-embeddable"))

    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(kotlinStdlib())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
