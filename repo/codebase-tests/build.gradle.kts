import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
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
    testImplementation("org.jetbrains.kotlin:test-federation-convention")
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
        "-Dgradle.user.home=${gradleUserHome.asFile.get().absolutePath}"
    )
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, javaLauncher = JdkMajorVersion.JDK_21_0) {
        dependsOn(":dist")
        dependsOn(":compileAll")
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
