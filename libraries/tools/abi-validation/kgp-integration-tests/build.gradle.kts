plugins {
    kotlin("jvm")
}

projectTest {
    // Disable KONAN_DATA_DIR env variable for all integration tests
    // because we are using `konan.data.dir` gradle property instead
    environment.remove("KONAN_DATA_DIR")

    dependsOnKotlinGradlePluginInstall()

    if (project.kotlinBuildProperties.isKotlinNativeEnabled) {
        // Build full Kotlin Native bundle
        dependsOn(":kotlin-native:install")
    }

    systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
}

dependencies {
    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(project(":kotlin-compiler-embeddable"))

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}
