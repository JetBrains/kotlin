plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":compiler:tests-common")))

    testImplementation(libs.jackson.dataformat.xml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.woodstox.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)

    testImplementation(libs.jgit)
}

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
    testTask(jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist")
        workingDir = rootDir
        javaLauncher.set(getToolchainLauncherFor(JdkMajorVersion.JDK_17_0))
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")

        jvmArgumentProviders.add(objects.newInstance<CodeOwnersArgumentProviders>().apply {
            spaceCodeOwnersFile.from(rootDir.resolve(".space/CODEOWNERS"))
        })
    }

    withJvmStdlibAndReflect()
}

testsJar()
