plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

description = "Integrated Swift Export Environment"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":native:swift:sir-light-classes"))
    implementation(project(":native:swift:sir-printer"))

    implementation(project(":analysis:analysis-api"))

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testRuntimeOnly(testFixtures(project(":analysis:low-level-api-fir")))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTests {
    nativeTestTask("test") {
        dependsOn(":dist", ":kotlin-native:distInvalidateStaleCaches")
    }

    withJvmStdlibAndReflect()
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()
