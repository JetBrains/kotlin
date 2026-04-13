plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("nodejs-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    testFixturesApi(project(":plugins:plugin-sandbox"))
    testFixturesApi(project(":compiler:incremental-compilation-impl"))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(testFixtures(project(":wasm:wasm.tests")))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))
    testFixturesApi(libs.junit.jupiter.api)

    testCompileOnly(intellijCore())

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    testRuntimeOnly(commonDependency("org.lz4:lz4-java"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(libs.intellij.fastutil)

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, maxHeapSizeMb = 3072) {
        dependsOn(":dist")
        workingDir = rootDir
        dependsOn(":plugins:plugin-sandbox:jar")
        dependsOn(":plugins:plugin-sandbox:plugin-annotations:distAnnotations")
        useJsIrBoxTests(buildDir = layout.buildDirectory)
        with(wasmNodeJsKotlinBuild) {
            setupNodeJs(nodejsVersion)
        }
        jvmArgumentProviders += objects.newInstance<AbsolutePathArgumentProvider>().apply {
            property.set("kotlin.wasm.test.root.out.dir")
            buildDirectory.set(layout.buildDirectory)
        }
    }

    testGenerator("org.jetbrains.kotlin.incremental.TestGeneratorForPluginSandboxICTestsKt")

    withJvmStdlibAndReflect()
    withJsRuntime()
    withWasmRuntime()
    withStdlibWeb()
    withStdlibCommon()
}

testsJar()
