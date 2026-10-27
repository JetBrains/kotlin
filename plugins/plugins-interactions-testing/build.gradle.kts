import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("d8-configuration")
    id("java-test-fixtures")
    id("test-inputs-check")
}

val beforePluginClasspath: Configuration = configurations.create("beforePluginClasspath")
val middlePluginClasspath: Configuration = configurations.create("middlePluginClasspath")
val afterPluginClasspath: Configuration = configurations.create("afterPluginClasspath")

val serializationCoreJsIrRuntimeForTests: Configuration = configurations.create("serializationCoreJsIrRuntimeForTests") {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}
val serializationJsonJsIrRuntimeForTests: Configuration = configurations.create("serializationJsonJsIrRuntimeForTests") {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

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

    serializationCoreJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0") { isTransitive = false }
    serializationJsonJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0") { isTransitive = false }
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

        useJsIrBoxTests(buildDir = layout.buildDirectory)
        addClasspathProperty(serializationCoreJsIrRuntimeForTests, "serialization.core.path")
        addClasspathProperty(serializationJsonJsIrRuntimeForTests, "serialization.json.path")
    }

    testGenerator("org.jetbrains.kotlin.compiler.plugins.TestGeneratorKt")

    testData(isolated, "testData")
    testData(project(":js:js.translator").isolated, "testData/_commonFiles")

    withJvmStdlibAndReflect()
    withPluginSandboxAnnotations()
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
