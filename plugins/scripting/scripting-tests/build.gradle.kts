
plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

val scriptingTestDefinition = configurations.create("scriptingTestDefinition")
val powerAssertCompilerPluginJar = configurations.create("powerAssertCompilerPluginJar")
val kotlinxSerializationGradlePluginClasspath = configurations.create("kotlinxSerializationGradlePluginClasspath")
val kotlinDataFrameGradlePluginClasspath = configurations.create("kotlinDataFrameGradlePluginClasspath")
val kotlinxCoroutinesCoreGradlePluginClasspath = configurations.create("kotlinxCoroutinesCoreGradlePluginClasspath")

dependencies {
    testFixturesApi(testFixtures(project(":compiler:tests-integration")))
    testFixturesApi(project(":kotlin-scripting-jvm"))
    testFixturesApi(project(":kotlin-scripting-compiler-impl"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(project(":compiler:fir:tree"))
    testFixturesImplementation(project(":analysis:light-classes-base"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testImplementation(project(":plugins:scripting:scripting-tests:runtime"))
    testImplementation(project(":kotlin-scripting-dependencies-maven"))
    testImplementation(libs.kotlinx.coroutines.core.jvm)
    testImplementation(kotlinTest("junit5"))

    testFixturesApi(platform(libs.junit.bom))
    testCompileOnly(project(":compiler:plugin-api"))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    scriptingTestDefinition(testFixtures(project(":plugins:scripting:test-script-definition")))
    powerAssertCompilerPluginJar(project(":kotlin-power-assert-compiler-plugin")) { isTransitive = false }
    kotlinxSerializationGradlePluginClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable")) { isTransitive = true }
    kotlinDataFrameGradlePluginClasspath(project(":kotlin-dataframe-compiler-plugin.embeddable")) { isTransitive = true }
    kotlinxCoroutinesCoreGradlePluginClasspath(libs.kotlinx.coroutines.core) { isTransitive = false }
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}


// Create a Gradle Task for the K2 example repl we can run from an IntelliJ Run Configuration
tasks.register<JavaExec>("runK2ExampleRepl") {
    val scriptingTestDefinitionClasspath = scriptingTestDefinition.asPath
    group = "application"
    workingDir = rootDir
    description = "Runs the K2 Example Repl"
    mainClass.set("org.jetbrains.kotlin.scripting.test.repl.example.ExampleReplKt")
    classpath = sourceSets.test.get().runtimeClasspath
    standardInput = System.`in`
    systemProperties["kotlin.script.test.script.definition.classpath"] = scriptingTestDefinitionClasspath
}

projectTests {
    testTask(defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0, JdkMajorVersion.JDK_21_0)) {
        workingDir = rootDir
        addClasspathProperty(testSourceSet.output.classesDirs, "kotlin.test.script.classpath")
        addClasspathProperty(powerAssertCompilerPluginJar, "kotlin.power.assert.compiler.plugin.jar")
        addClasspathProperty(kotlinxSerializationGradlePluginClasspath, "kotlin.script.test.kotlinx.serialization.plugin.classpath")
        addClasspathProperty(kotlinDataFrameGradlePluginClasspath, "kotlin.script.test.kotlin.dataframe.plugin.classpath")
        addClasspathProperty(kotlinxCoroutinesCoreGradlePluginClasspath, "kotlin.script.test.kotlinx.coroutines.core.classpath")
    }

    testGenerator("org.jetbrains.kotlin.scripting.test.TestGeneratorKt")
    testData(isolated, "testData")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withScriptingPlugin()
    withScriptingTestsRuntime()
    withMainKtsJar()
    withAllOpenCompilerPluginJar()
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
    withTestScriptDefinition()
}

testsJar()
