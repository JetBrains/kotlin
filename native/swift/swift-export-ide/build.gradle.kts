plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
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

    testRuntimeOnly(testFixtures(project(":analysis:low-level-api-fir")))

    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(project(":native:swift:sir"))
    testFixturesImplementation(project(":native:swift:sir-providers"))
    testFixturesImplementation(project(":native:swift:sir-printer"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(isolated, "testData")

    nativeTestTask(
        "test",
        allowUnsafe = true, // KT-85212
    ) {
        dependsOn(":kotlin-native:distInvalidateStaleCaches")
        extensions.configure<TestInputsCheckExtension>("testInputsCheck") {
            allowFlightRecorder.set(true)
        }
    }

    testGenerator(
        "org.jetbrains.kotlin.swiftexport.ide.TestGeneratorKt",
        generateTestsInBuildDirectory = true,
    )

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
