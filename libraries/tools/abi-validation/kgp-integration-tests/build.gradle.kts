plugins {
    kotlin("jvm")
}

projectTest {
    // Disable KONAN_DATA_DIR env variable for all integration tests
    // because we are using `konan.data.dir` gradle property instead
    environment.remove("KONAN_DATA_DIR")

    // for some reason if tests are running on ARM MacOS Gradle can take x64 JVM, override it by explicit passing JVM home dir
    if (System.getProperty("os.arch") == "aarch64") {
        systemProperty("org.gradle.java.home", System.getProperty("java.home"))
    }

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
