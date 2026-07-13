import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("test-batches-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(kotlin("test-junit5", libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    implementation(kotlin("tooling-core", version = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))

    testImplementation(libs.jackson.dataformat.xml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.woodstox.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.jgit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures("org.jetbrains.kotlin:repo-test-fixtures"))
    testImplementation("org.jetbrains.kotlin:test-federation-convention")
    testImplementation(testFederationRuntime)
    testImplementation("org.jetbrains.kotlin:buildsrc-compat") {
        isTransitive = false
    }
    testImplementation(gradleTestKit())
    testImplementation(libs.intellij.asm)
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
        "-Dgradle.user.home=${gradleUserHome.asFile.get().absolutePath}",
    )
}

projectTests {
    testTask(javaLauncher = JdkMajorVersion.JDK_21_0, maxHeapSizeMb = 128) {
        dependsOn(":dist")
        dependsOn(":compileAll")
        workingDir = rootDir
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")

        jvmArgumentProviders.add(objects.newInstance<TestSystemPropertiesProvider>().apply {
            spaceCodeOwnersFile.from(rootDir.resolve(".space/CODEOWNERS"))
            gradleUserHome.set(gradle.gradleUserHomeDir)
        })

        smokeTestConfig = SmokeTestConfig.RunAllTests
        forkEvery = 1
    }

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
}

testsJar()

tasks.register<JavaExec>("updateTestLifecycleTaskDump") {
    dependsOn(":compileAll")
    description = "Updates the 'testLifecycleTask.dump.txt' file"
    classpath = project.files(sourceSets.test.map { it.runtimeClasspath })
    mainClass.set($$"org.jetbrains.kotlin.code.TestLifecycleTaskTest$Update")
    workingDir = rootDir
}

tasks.register<JavaExec>("updateDomainsDump") {
    doNotTrackState("Should always run")
    description = "Updates the 'domains.dump.txt' file"
    classpath = files(sourceSets.test.map { it.runtimeClasspath })
    workingDir = rootDir
    mainClass = $$"org.jetbrains.kotlin.code.DomainsDumpTest$Update"
}

tasks.configureEach {
    if (this !is JavaForkOptions) return@configureEach

    /* The dump includes tests from Kotlin/Native, updating it requires those parts of the build to be enabled */
    val isKotlinNativeEnabled = kotlinBuildProperties.isKotlinNativeEnabled
    val projectPath = project.path
    doFirst {
        if (!isKotlinNativeEnabled.get()) error(
            "Running '$projectPath' requires Kotlin/Native to be enabled (-Pkotlin.native.enabled=true)"
        )
    }

    /* Nested/Deep debugging support */
    val debuggerDispatchPort = providers.systemProperty("idea.debugger.dispatch.port")
    inputs.property("idea.debugger.dispatch.port", debuggerDispatchPort).optional(true)

    doFirst {
        if (debuggerDispatchPort.isPresent) {
            systemProperty("idea.debugger.dispatch.port", debuggerDispatchPort.get())
        }
    }
}

/* Create synthetic test tasks */
run {
    val junit5TestCompilation = kotlin.target.compilations.create("junit5Tests")

    tasks.register<Test>("junit5Tests") {
        description = "Synthetic Tests: Used by functional tests to create test build behavior (on junit5)"
        useJUnitPlatform()
        testClassesDirs = junit5TestCompilation.output.classesDirs
        classpath = junit5TestCompilation.runtimeDependencyFiles

        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    dependencies {
        junit5TestCompilation.configurations.implementationConfiguration(kotlin("test-junit5"))
        junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.api)
        junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.engine)
        junit5TestCompilation.configurations.implementationConfiguration(libs.junit.jupiter.params)
    }
}
