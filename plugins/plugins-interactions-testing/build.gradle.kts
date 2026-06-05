plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val beforePluginClasspath: Configuration = configurations.create("beforePluginClasspath")
val middlePluginClasspath: Configuration = configurations.create("middlePluginClasspath")
val afterPluginClasspath: Configuration = configurations.create("afterPluginClasspath")

dependencies {
    testFixturesApi(testFixtures(project(":kotlin-allopen-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-assignment-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlinx-serialization-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-lombok-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-noarg-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-power-assert-compiler-plugin")))
    testFixturesApi(testFixtures(project(":plugins:parcelize:parcelize-compiler")))
    testFixturesApi(testFixtures(project(":plugins:plugin-sandbox")))

    testFixturesApi(testFixtures(project(":compiler:tests-integration")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }

    beforePluginClasspath(project(":plugins:test-plugins:before"))
    middlePluginClasspath(project(":plugins:test-plugins:middle"))
    afterPluginClasspath(project(":plugins:test-plugins:after"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

runtimeJar()
sourcesJar()
testsJar()

projectTests {
    testTask(
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,
            JdkMajorVersion.JDK_11_0,
            JdkMajorVersion.JDK_17_0,
            JdkMajorVersion.JDK_21_0
        )
    ) {
        useJUnitPlatform()

        addClasspathProperty(beforePluginClasspath, "plugin.classpath.before")
        addClasspathProperty(middlePluginClasspath, "plugin.classpath.middle")
        addClasspathProperty(afterPluginClasspath, "plugin.classpath.after")
    }

    testGenerator("org.jetbrains.kotlin.compiler.plugins.TestGeneratorKt")

    testData(isolated, "testData")

    withJvmStdlibAndReflect()
    withPluginSandboxAnnotations()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withStdlibCommon()
    withThirdPartyAnnotations()
    withThirdPartyJsr305()
    withThirdPartyJava8Annotations()
    withStdlibCommon()
    withJsRuntime()
    withWasmRuntime()
}
