plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":compiler:tests-common")))

    testImplementation(libs.jackson.dataformat.xml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.woodstox.core)
    testApi(platform(libs.junit.bom))
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
    val scriptFile: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val spaceCodeOwnersFile: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val virtualTeamMappingFile: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val githubCodeOwnersFile: ConfigurableFileCollection = objectFactory.fileCollection()

    override fun asArguments(): Iterable<String> = listOf(
        "-DcodeOwnersTest.scriptFile=${scriptFile.singleFile.absolutePath}",
        "-DcodeOwnersTest.spaceCodeOwnersFile=${spaceCodeOwnersFile.singleFile.absolutePath}",
        "-DcodeOwnersTest.virtualTeamMappingFile=${virtualTeamMappingFile.singleFile.absolutePath}",
        "-DcodeOwnersTest.githubCodeOwnersFile=${githubCodeOwnersFile.singleFile.absolutePath}"
    )
}

projectTest(jUnitMode = JUnitMode.JUnit4) {
    dependsOn(":dist")
    workingDir = rootDir
    javaLauncher.set(getToolchainLauncherFor(JdkMajorVersion.JDK_17_0))
    jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")

    jvmArgumentProviders.add(objects.newInstance<CodeOwnersArgumentProviders>().apply {
        scriptFile.from(rootDir.resolve(".space/generate-github-codeowners.sh"))
        spaceCodeOwnersFile.from(rootDir.resolve(".space/CODEOWNERS"))
        virtualTeamMappingFile.from(rootDir.resolve(".space/codeowners-virtual-team-mapping.json"))
        githubCodeOwnersFile.from(rootDir.resolve(".github/CODEOWNERS"))
    })
}

testsJar()
