
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
    testImplementation(kotlinTest("junit5"))

    testFixturesApi(platform(libs.junit.bom))
    testCompileOnly(project(":compiler:plugin-api"))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    scriptingTestDefinition(testFixtures(project(":plugins:scripting:test-script-definition")))
    powerAssertCompilerPluginJar(project(":kotlin-power-assert-compiler-plugin")) { isTransitive = false }
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
        generatedTestDir()
        java.srcDir("tests-organized")
    }
    "testFixtures" {projectDefault() }
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
