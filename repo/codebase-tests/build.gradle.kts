import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    testRuntimeOnly(libs.junit.vintage.engine)

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

open class CodeOwnersArgumentProviders @Inject constructor(
    objectFactory: ObjectFactory
) : CommandLineArgumentProvider {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val spaceCodeOwnersFile: ConfigurableFileCollection = objectFactory.fileCollection()

    override fun asArguments(): Iterable<String> = listOf(
        "-DcodeOwnersTest.spaceCodeOwnersFile=${spaceCodeOwnersFile.singleFile.absolutePath}",
    )
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, javaLauncher = JdkMajorVersion.JDK_21_0) {
        dependsOn(":dist")
        workingDir = rootDir
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")

        jvmArgumentProviders.add(objects.newInstance<CodeOwnersArgumentProviders>().apply {
            spaceCodeOwnersFile.from(rootDir.resolve(".space/CODEOWNERS"))
        })

        smokeTestConfig = SmokeTestConfig.RunAllTests
    }

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
}

testsJar()
