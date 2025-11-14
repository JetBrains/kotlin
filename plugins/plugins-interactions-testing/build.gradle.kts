plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

val beforePluginClasspath: Configuration by configurations.creating
val middlePluginClasspath: Configuration by configurations.creating
val afterPluginClasspath: Configuration by configurations.creating

dependencies {
    testFixturesApi(testFixtures(project(":kotlin-allopen-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-assignment-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlinx-serialization-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-lombok-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-noarg-compiler-plugin")))
    testFixturesApi(testFixtures(project(":plugins:parcelize:parcelize-compiler")))

    testFixturesApi(testFixtures(project(":compiler:tests-integration")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

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
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

runtimeJar()
sourcesJar()
testsJar()

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0, JdkMajorVersion.JDK_21_0)
    ) {
        dependsOn(":dist")
        workingDir = rootDir
        useJUnitPlatform()

        addClasspathProperty(beforePluginClasspath, "plugin.classpath.before")
        addClasspathProperty(middlePluginClasspath, "plugin.classpath.middle")
        addClasspathProperty(afterPluginClasspath, "plugin.classpath.after")
    }

    testGenerator("org.jetbrains.kotlin.compiler.plugins.TestGeneratorKt")

    withJvmStdlibAndReflect()
}
