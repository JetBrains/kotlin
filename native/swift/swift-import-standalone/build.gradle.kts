plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

description = "Standalone Runner for Swift Import"

kotlin {
    explicitApi()
}

project.configureJvmToolchain(JdkMajorVersion.JDK_25_0)

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))
    implementation(variantOf(libs.swiftextract) { classifier("osx-aarch_64") })

    testImplementation(kotlinStdlib())
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.api)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask(javaLauncher = JdkMajorVersion.JDK_25_0) {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}
