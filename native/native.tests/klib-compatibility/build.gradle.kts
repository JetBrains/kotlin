import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDirectories

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(libs.junit.vintage.engine)

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))

    testFixturesApi(testFixtures(project(":native:native.tests:klib-ir-inliner")))
}

sourceSets {
    "main" { }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

tasks.test {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

fun Test.setUpNativeBoxTests(tag: String) {
    dependsOn(":dist")
    useJUnitPlatform { includeTags(tag) }
    workingDir = rootDir
}

val hostSpecificArtifact = "${HostManager.platformName()}@${if (HostManager.hostIsMingw) "zip" else "tar.gz"}"

fun Project.customCompilerTest(
    version: CustomCompilerVersion,
    taskName: String,
    tag: String,
    body: Test.() -> Unit = {},
): TaskProvider<out Task> {
    val customCompiler: Configuration = getOrCreateConfiguration("customCompiler_$version") {
        project.dependencies.add(name, "org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:$hostSpecificArtifact")
        dependencies {
            // declared to be included in verification-metadata.xml, to be executed on developer's and CI computers.
            implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:macos-aarch64@tar.gz")
            implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:linux-x86_64@tar.gz")
        }
    }
    // Cannot use exactly `DependencyDirectories.localKonanDir`, since it's wrong to declare whole `~/.konan/` as an output of `unarchiveCustomCompiler_` task
    // Should it be so, Gradle fails on implicit dependency: task `:kotlin-native:llvmInterop:genInteropStubs` uses files in `~/.konan/dependencies/llvm-19-aarch64*`
    // So, a subfolder within `~/.konan/` is needed for output of `unarchiveCustomCompiler_` task
    val customCompilersCollectionDir = DependencyDirectories.localKonanDir.resolve("kotlin-native-prebuilt-releases")
    val unarchiveCustomCompiler = tasks.register("unarchiveCustomCompiler_${taskName}", Sync::class) {
        val archiveTree = { archive: File -> if (HostManager.hostIsMingw) zipTree(archive) else tarTree(archive) }
        from(archiveTree(customCompiler.singleFile))
        into(customCompilersCollectionDir)
    }
    return projectTests.nativeTestTask(
        taskName,
        allowParallelExecution = true,
        requirePlatformLibs = false,
    ) {
        useJUnitPlatform { includeTags(tag) }
        extensions.configure<TestInputsCheckExtension> {
            isNative.set(true)
        }

        dependsOn(unarchiveCustomCompiler)
        inputs.files(unarchiveCustomCompiler.get().outputs)
        doFirst {
            val customCompilerDir = customCompilersCollectionDir.listFiles()?.filter {
                it.isDirectory && it.name.toString().endsWith(version.rawVersion)
            }?.first()
            if (customCompilerDir == null || !customCompilerDir.exists())
                error ("Folder `$customCompilersCollectionDir` must have a subfolder with version ${version.rawVersion} of K/N prebuilt compiler")

            systemProperty("kotlin.internal.native.test.compat.customCompilerDist", customCompilerDir.absolutePath)
            val konanLibDir = customCompilerDir.resolve("konan/lib")
            val runtimeJars = listOf("kotlin-native-compiler-embeddable.jar", "trove4j.jar")
            systemProperty(
                "kotlin.internal.native.test.compat.customCompilerClasspath",
                runtimeJars.joinToString(File.pathSeparator) { konanLibDir.resolve(it).absolutePath }
            )
            systemProperty("kotlin.internal.native.test.compat.customCompilerVersion", version.rawVersion)
            systemProperty(
                "kotlin.internal.native.test.compat.currentCompilerDist",
                rootProject.projectDir.resolve("kotlin-native/dist")
            )
        }
        body()
    }
}

data class CustomCompilerVersion(val rawVersion: String) {
    val sanitizedVersion = rawVersion.replace('.', '_').replace('-', '_')
    override fun toString() = sanitizedVersion
}

fun Project.customFirstStageTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)
    return customCompilerTest(
        version = version,
        taskName = "testCustomFirstStage_$version",
        tag = "custom-first-stage"
    )
}

fun Project.customSecondStageTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)
    return customCompilerTest(
        version = version,
        taskName = "testCustomSecondStage_$version",
        tag = "custom-second-stage"
    )
}

customFirstStageTest("1.9.20")
customFirstStageTest("2.0.0")
customFirstStageTest("2.1.0")
customFirstStageTest("2.2.0")
customFirstStageTest("2.3.0")

customSecondStageTest("2.2.0")
customSecondStageTest("2.3.0")

projectTests {
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateNativeKlibCompatibilityTestsKt", generateTestsInBuildDirectory = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
    testData(project(":compiler").isolated, "testData/codegen/box")
    testData(project(":compiler").isolated, "testData/codegen/boxInline")

    // Permissions for older compiler, for unnecessarily performed access to root dir, already fixed in 2.2.20, commit dbd8ac94
    testData(rootProject.isolated, "stdlib")
    testData(rootProject.isolated, "stdlib.klib")
}
