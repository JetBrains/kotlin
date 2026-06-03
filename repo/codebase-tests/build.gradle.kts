import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(kotlin("test-junit5", libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))

    testImplementation(libs.jackson.dataformat.xml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.woodstox.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.jgit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures("org.jetbrains.kotlin:repo-test-fixtures"))
    testImplementation(gradleTestKit())
}

configureJvmToolchain(JdkMajorVersion.JDK_21_0)

sourceSets {
    "main" {}
    "test" {
        projectDefault()
    }
}

open class TestSystemPropertiesProvider @Inject constructor(
    objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val spaceCodeOwnersFile: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Internal
    val gradleUserHome: DirectoryProperty = objectFactory.directoryProperty()

    override fun asArguments(): Iterable<String> = listOf(
        "-DcodeOwnersTest.spaceCodeOwnersFile=${spaceCodeOwnersFile.singleFile.absolutePath}",
        "-Dgradle.user.home=${gradleUserHome.asFile.get().absolutePath}"
    )
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, javaLauncher = JdkMajorVersion.JDK_21_0) {
        dependsOn(":dist")
        workingDir = rootDir
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
        withJunit5ParallelExecution(2)

        jvmArgumentProviders.add(objects.newInstance<TestSystemPropertiesProvider>().apply {
            spaceCodeOwnersFile.from(rootDir.resolve(".space/CODEOWNERS"))
            gradleUserHome.set(gradle.gradleUserHomeDir)
        })

        smokeTestConfig = SmokeTestConfig.RunAllTests
    }

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
}

testsJar()


/* Create synthetic test tasks */
val junit5TestCompilation = kotlin.target.compilations.create("junit5Tests")
val junit4TestCompilation = kotlin.target.compilations.create("junit4Tests")

tasks.register<Test>("junit5Tests") {
    description = "Synthetic Tests: Used by functional tests to create test build behavior (on junit5)"
    useJUnitPlatform()
    testClassesDirs = junit5TestCompilation.output.classesDirs
    classpath = junit5TestCompilation.runtimeDependencyFiles
}

tasks.register<Test>("junit4Tests") {
    description = "Synthetic Tests: Used by functional tests to create test build behavior (on junit4)"
    useJUnit()
    testClassesDirs = junit4TestCompilation.output.classesDirs
    classpath = junit4TestCompilation.runtimeDependencyFiles
}

tasks.withType<Test>().configureEach {
    environment
    testLogging {
        events("passed", "skipped", "failed")
    }

    /* Nested/Deep debugging support */
    val debuggerDispatchPort = providers.systemProperty("idea.debugger.dispatch.port")
    val additionalTestJvmArgument = providers.gradleProperty("tests.additionalJvmArgument")
    inputs.property("idea.debugger.dispatch.port", debuggerDispatchPort).optional(true)
    inputs.property("tests.additionalJvmArgument", additionalTestJvmArgument).optional(true)

    doFirst {
        if (debuggerDispatchPort.isPresent) {
            systemProperty("idea.debugger.dispatch.port", debuggerDispatchPort.get())
        }

        if (additionalTestJvmArgument.isPresent) {
            jvmArgs(additionalTestJvmArgument.get())
        }
    }
}

dependencies {
    junit5TestCompilation.configurations.implementationConfiguration(kotlin("test-junit5"))
    junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.api)
    junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.engine)
    junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.params)

    junit4TestCompilation.configurations.implementationConfiguration(kotlin("test-junit"))
    junit4TestCompilation.configurations.implementationConfiguration(libs.junit4)
}
