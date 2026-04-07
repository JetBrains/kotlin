plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("nodejs-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
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
        useJsIrBoxTests(buildDir = layout.buildDirectory)
        with(wasmNodeJsKotlinBuild) {
            setupNodeJs(nodejsVersion)
        }
        jvmArgumentProviders += objects.newInstance<AbsolutePathArgumentProvider>().apply {
            property.set("kotlin.wasm.test.root.out.dir")
            buildDirectory.set(layout.buildDirectory)
        }
        extensions.configure<TestInputsCheckExtension> {
            with(extraPermissions) {
                add("permission java.util.PropertyPermission \"kotlin.incremental.compilation\", \"write\";")
                add("permission java.util.PropertyPermission \"kotlin.incremental.compilation.js\", \"write\";")
                // The plugin-sandbox compiler plugin generates synthetic source files (like AllOpenGenerated.kt),
                // and later on the compiler asserts that the synthetic file does not exist via !File.exists()
                add("""permission java.io.FilePermission "${projectDir.absolutePath}/-", "read";""")
            }
        }
    }

    testGenerator("org.jetbrains.kotlin.incremental.TestGeneratorForPluginSandboxICTestsKt")

    withJvmStdlibAndReflect()
    withJsRuntime()
    withWasmRuntime()
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
    withMockJdkAnnotationsJar()
    withPluginSandboxJar()
    withPluginSandboxAnnotations()

    testData(project.isolated, "testData")
    testData(project(":js:js.translator").isolated, "testData")
}

testsJar()
