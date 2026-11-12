plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

projectTests {
    testTask()
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":compiler:cli"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli:cli-native-klib"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.backend.native"))
    implementation(project(":compiler:util"))
    implementation(project(":kotlin-native:backend.native"))
    implementation(project(":native:frontend.native"))
    implementation(project(":native:native.config"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

sourcesJar()
javadocJar()
