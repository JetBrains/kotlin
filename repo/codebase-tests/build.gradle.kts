import org.jetbrains.kotlin.testFederation.testFederation

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(testFixtures("org.jetbrains.kotlin:repo-test-fixtures"))
    implementation("org.jetbrains.kotlin:test-federation-convention")
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:buildsrc-compat") {
        isTransitive = false
    }
    runtimeOnly(libs.log4j2.slf4j.impl)

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
    testImplementation(project(":repo:test-runtime"))
    testImplementation(gradleTestKit())
    testImplementation(libs.intellij.asm)
    testImplementation(libs.icu4j)
}

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_21_0
    targetBytecodeVersion = JdkMajorVersion.JDK_21_0
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
    }
    /* Tests that inspect compiled classes, dist or run Gradle; executed by the `withBinariesTest` task */
    "withBinariesTest" {
        codebaseTestDir(project, "withBinariesTest")
    }
    /* Long-running tests; executed by the `nightlyTests` task */
    "nightlyTests" {
        codebaseTestDir(project, "nightlyTests")
    }
}

/* Gives the tests from the additional source sets access to `internal` declarations of `main` */
for (sourceSetName in listOf("withBinariesTest", "nightlyTests")) {
    kotlin.target.compilations.named(sourceSetName) {
        associateWith(kotlin.target.compilations.getByName("main"))
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

    @get:Input
    @get:Optional
    val qualityGateMasterTasks = objectFactory.property<String>()

    @get:Input
    @get:Optional
    val qualityGateNightlyTasks = objectFactory.property<String>()

    @get:Input
    val teamcity = objectFactory.property<Boolean>()

    override fun asArguments(): Iterable<String> = listOfNotNull(
        spaceCodeOwnersFile.takeIf { it.files.isNotEmpty() }?.let { "-DcodeOwnersTest.spaceCodeOwnersFile=${it.singleFile.absolutePath}" },
        "-Dgradle.user.home=${gradleUserHome.asFile.get().absolutePath}",
        "-Dteamcity=${teamcity.get()}",
        qualityGateMasterTasks.orNull?.let { "-Dquality.gate.master.tasks=$it" },
        qualityGateNightlyTasks.orNull?.let { "-Dquality.gate.nightly.tasks=$it" },
    )
}

projectTests {
    testTask(javaLauncher = JdkMajorVersion.JDK_21_0, maxHeapSize = testMaxHeapSizeTiny) {
        description = "Runs tests that checks files in the repository"
        workingDir = rootDir

        jvmArgumentProviders.add(objects.newInstance<TestSystemPropertiesProvider>().apply {
            spaceCodeOwnersFile.from(rootDir.resolve(".space/CODEOWNERS"))
            gradleUserHome.set(gradle.gradleUserHomeDir)
            teamcity = kotlinBuildProperties.isTeamcityBuild
        })

        testFederation {
            smokeTests { includeAll() }
        }
    }

    testTask("withBinariesTest", javaLauncher = JdkMajorVersion.JDK_21_0, maxHeapSize = testMaxHeapSizeTiny, skipInLocalBuild = false) {
        description = "Runs codebase tests from the 'withBinariesTest' source set, which depend on the compilation of the project and dist"
        if (!dryRun.get()) {
            dependsOn(":dist", ":compileAll")
        }

        val withBinariesTestSourceSet = sourceSets["withBinariesTest"]
        testClassesDirs = withBinariesTestSourceSet.output.classesDirs
        classpath = withBinariesTestSourceSet.runtimeClasspath

        workingDir = rootDir

        withJvmStdlibAndReflect()
        withTestJar()

        jvmArgumentProviders.add(objects.newInstance<TestSystemPropertiesProvider>().apply {
            spaceCodeOwnersFile.from(rootDir.resolve(".space/CODEOWNERS"))
            gradleUserHome.set(gradle.gradleUserHomeDir)
            teamcity = kotlinBuildProperties.isTeamcityBuild
            qualityGateMasterTasks.set(providers.gradleProperty("quality.gate.master.tasks"))
            qualityGateNightlyTasks.set(providers.gradleProperty("quality.gate.nightly.tasks"))
        })

        testFederation {
            smokeTests { includeAll() }
        }

        assertKotlinNativeIsEnabled()
    }

    testTask("nightlyTests", javaLauncher = JdkMajorVersion.JDK_21_0, skipInLocalBuild = false) {
        val nightlyTestsSourceSet = sourceSets["nightlyTests"]
        testClassesDirs = nightlyTestsSourceSet.output.classesDirs
        classpath = nightlyTestsSourceSet.runtimeClasspath

        workingDir = rootDir

        jvmArgumentProviders.add(objects.newInstance<TestSystemPropertiesProvider>().apply {
            gradleUserHome.set(gradle.gradleUserHomeDir)
            teamcity = kotlinBuildProperties.isTeamcityBuild
        })

        forkEvery = 1

        assertKotlinNativeIsEnabled()
    }
}

tasks.register<JavaExec>("updateTestLifecycleTaskDump") {
    doNotTrackState("Should always run")
    description = "Updates the 'testLifecycleTask.dump.txt' file"
    classpath = files(sourceSets.main.map { it.runtimeClasspath })
    workingDir = rootDir
    mainClass = "org.jetbrains.kotlin.code.UpdateTestLifecycleTaskDump"
}

tasks.register<JavaExec>("updateDomainsDump") {
    doNotTrackState("Should always run")
    description = "Updates the 'domains.dump.txt' and 'domains.ignore.dump.txt' files"
    classpath = files(sourceSets.main.map { it.runtimeClasspath })
    workingDir = rootDir
    mainClass = "org.jetbrains.kotlin.code.UpdateDomainsDump"
}

fun Task.assertKotlinNativeIsEnabled() {
    val isKotlinNativeEnabled = kotlinBuildProperties.isKotlinNativeEnabled
    doFirst {
        if (!isKotlinNativeEnabled.get()) error(
            "Running '$path' requires Kotlin/Native to be enabled (-Pkotlin.native.enabled=true)"
        )
    }
}

tasks.configureEach {
    if (this !is JavaForkOptions) return@configureEach

    /* Nested/Deep debugging support */
    val debuggerDispatchPort = providers.systemProperty("idea.debugger.dispatch.port")
    inputs.property("idea.debugger.dispatch.port", debuggerDispatchPort).optional(true)

    doFirst {
        if (debuggerDispatchPort.isPresent) {
            systemProperty("idea.debugger.dispatch.port", debuggerDispatchPort.get())
        }
    }
}
