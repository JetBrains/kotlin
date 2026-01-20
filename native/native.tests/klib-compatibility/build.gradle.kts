import org.gradle.api.tasks.Copy
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
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

tasks.test {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

fun Project.customCompilerTest(
    version: CustomCompilerVersion,
    taskName: String,
    tag: String,
    body: Test.() -> Unit = {},
): TaskProvider<out Task> {
    if (HostManager.hostIsMingw) {
        // Klib compatibility tests are intended to run on MacOS(for some developers) and Linux(for some developers and CI),
        // Windows-specific artifacts have `@zip` extensions instead of `@tar.gz`, and contain zip files instead of tar.
        // So the whole task is simply skipped for the simplicity.
        return tasks.register(taskName) {
            enabled = false
        }
    }
    val customCompiler: Configuration = getOrCreateConfiguration("customCompiler_$version") {
        project.dependencies.add(
            name,
            "org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:${HostManager.platformName()}@tar.gz"
        )
        dependencies {
            // declared to be included in verification-metadata.xml, to be executed on developer's and CI computers.
            implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:macos-aarch64@tar.gz")
            implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:linux-x86_64@tar.gz")
        }
    }
    // Cannot use exactly `DependencyDirectories.localKonanDir`, since it's wrong to declare whole `~/.konan/` as an output of `unarchiveCustomCompiler_` task
    // Should it be so, Gradle fails on implicit dependency: task `:kotlin-native:llvmInterop:genInteropStubs` uses files in `~/.konan/dependencies/llvm-19-aarch64*`
    // So, a subfolder within `~/.konan/` is needed for output of `unarchiveCustomCompiler_` task
    val unarchiveCustomCompiler = tasks.register("unarchiveCustomCompiler_${taskName}", Copy::class) {
        from(customCompiler.map { file -> tarTree(file) }.single())
        into(DependencyDirectories.localKonanDir.resolve("kotlin-native-prebuilt-releases"))
    }
    return projectTests.nativeTestTask(
        taskName,
        allowParallelExecution = true,
        requirePlatformLibs = false,
    ) {
        useJUnitPlatform { includeTags(tag) }
        extensions.configure<TestInputsCheckExtension> {
            isNative.set(true)
            // Permissions for older compiler, for unnecessarily performed access to root dir, already fixed in 2.2.20, commit dbd8ac94
            extraPermissions.add("""permission java.io.FilePermission "${rootDir.resolve("stdlib")}", "read";""")
            extraPermissions.add("""permission java.io.FilePermission "${rootDir.resolve("stdlib.klib")}", "read";""")
            // add link permission to load `libcallbacks.dylib`, via possible invocation of `JvmUtilsKt.createTempDirWithLibrary()` which invokes `Files.createLink()`
            extraPermissions.add("""permission java.nio.file.LinkPermission "hard";""")
        }
        val rawVersion = version.rawVersion

        val unarchiveCustomCompilerFiles: File = unarchiveCustomCompiler.get().outputs.files.singleFile
        inputs.files(unarchiveCustomCompiler)
            .withPropertyName("unarchiveCustomCompiler")
            .withNormalizer(ClasspathNormalizer::class.java)

        doFirst {
            val customCompilerDirProvider: File? = unarchiveCustomCompilerFiles.listFiles()?.first {
                it.isDirectory && it.name.toString().endsWith(rawVersion)
            }
            check(customCompilerDirProvider != null && customCompilerDirProvider.exists()) { "Folder `${customCompilerDirProvider}` must have a subfolder with version $rawVersion of K/N prebuilt compiler" }
            systemProperty("kotlin.internal.native.test.compat.customCompilerDist", customCompilerDirProvider.absolutePath)
            val konanLibDir = customCompilerDirProvider.resolve("konan/lib")
            val runtimeJars = listOf("kotlin-native-compiler-embeddable.jar", "trove4j.jar")
            systemProperty(
                "kotlin.internal.native.test.compat.customCompilerClasspath",
                runtimeJars.map { jar -> konanLibDir.resolve(jar) }.joinToString(File.pathSeparator)
            )
        }
        doLast {
            systemProperties.remove("kotlin.internal.native.test.compat.customCompilerDist")
            systemProperties.remove("kotlin.internal.native.test.compat.customCompilerClasspath")
        }
        systemProperty("kotlin.internal.native.test.compat.customCompilerVersion", version.rawVersion)
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

fun Project.customStagesAggregateTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)
    return customCompilerTest(
        version = version,
        taskName = "testMinimalInAggregate",
        tag = "aggregate"
    )
}

customFirstStageTest("1.9.20")
customFirstStageTest("2.0.0")
customFirstStageTest("2.1.0")
customFirstStageTest("2.2.0")
customFirstStageTest("2.3.0")
// TODO: Add a new task for the "custom-first-stage" test here.

/* Custom-second-stage test task for the two compiler major versions: previous one and the latest one . */
// TODO: Keep updating two following compiler versions to be the previous and latest ones.
customSecondStageTest("2.3.0")
// add `customSecondStageTest("2.4.0-Beta1")`, as soon it is released

// TODO: Keep updating the following compiler versions to be the previous major one.
customStagesAggregateTest("2.3.0")

projectTests {
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateNativeKlibCompatibilityTestsKt", generateTestsInBuildDirectory = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
    testData(project(":compiler").isolated, "testData/codegen/box")
    testData(project(":compiler").isolated, "testData/codegen/boxInline")
    testData(project(":compiler").isolated, "testData/klib/klib-compatibility/sanity")
}
