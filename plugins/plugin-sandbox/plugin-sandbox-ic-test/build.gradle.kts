plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("d8-configuration")
    id("nodejs-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    testFixturesApi(project(":plugins:plugin-sandbox"))
    testFixturesApi(project(":compiler:incremental-compilation-impl"))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(testFixtures(project(":wasm:wasm.tests")))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))
    testFixturesImplementation(project(":wasm:wasm.frontend"))
    testFixturesApi(libs.junit.jupiter.api)

    testCompileOnly(intellijCore())

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    testRuntimeOnly(commonDependency("org.lz4:lz4-java"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(libs.intellij.fastutil)

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":compiler:cli-base"))
}

sourceSets {
    main { none() }
    test {
        projectDefault()
        generatedTestDir()
    }
    testFixtures { projectDefault() }
}

projectTests {
    testTask(maxHeapSizeMb = 3072) {
        useJsIrBoxTests(buildDir = layout.buildDirectory)
        wasmNodeJsKotlinBuild {
            setupNodeJs(nodejsVersion)
        }
        addAbsoluteDirectoryProperty(layout.buildDirectory, "kotlin.wasm.test.root.out.dir")
    }

    testGenerator("org.jetbrains.kotlin.incremental.TestGeneratorForPluginSandboxICTestsKt")

    withJvmStdlibAndReflect()
    withJsRuntime()
    withWasmRuntime()
    withMockJdkAnnotationsJar()
    withPluginSandboxJar()
    withPluginSandboxAnnotations()

    testData(project.isolated, "testData")
    testData(project(":js:js.translator").isolated, "testData/moduleEmulation.js")
}

testsJar()
