plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        // Disable KONAN_DATA_DIR env variable for all integration tests
        // because we are using `konan.data.dir` gradle property instead
        // environment is now a MapProperty; rebuild it without the key to drop KONAN_DATA_DIR
        environment.set(environment.get() - "KONAN_DATA_DIR")

        dependsOnKotlinGradlePluginInstall()

        if (project.kotlinBuildProperties.isKotlinNativeEnabled.get()) {
            // Build full Kotlin Native bundle
            dependsOn(":kotlin-native:install")
        }

        systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    }
}

dependencies {
    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(project(":kotlin-compiler-embeddable"))

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}
