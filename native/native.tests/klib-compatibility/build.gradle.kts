import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
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

//testsJar {}

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
): TaskProvider<out Task> {
    val customCompiler: Configuration = getOrCreateConfiguration("customCompiler_$version") {
        project.dependencies.add(name, "org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:$hostSpecificArtifact")
        dependencies {
            // declared to be included in verification-metadata.xml
            implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:macos-aarch64@tar.gz")
            implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:linux-x86_64@tar.gz")
            implicitDependencies("org.jetbrains.kotlin:kotlin-native-prebuilt:${version.rawVersion}:windows-x86_64@zip")
        }
    }
    val unarchiveCustomCompiler = tasks.register("unarchiveCustomCompiler_${taskName}_$version", Sync::class) {
        // Maybe download/unarchiving can be done using `NativeCompilerDownloader`, to get it in `~/.konan/kotlin-native-prebuilt-*`?
        val unarchive = { archive: File -> if (HostManager.hostIsMingw) zipTree(archive) else tarTree(archive) }
        from(unarchive(customCompiler.singleFile))
        val destDir = layout.buildDirectory.dir("customCompiler_$version")
        into(destDir)
    }
    return projectTests.nativeTestTask(
        taskName,
        allowParallelExecution = true,
        requirePlatformLibs = false,
    ) {
        dependsOn(unarchiveCustomCompiler)
        useJUnitPlatform { includeTags(tag) }
        val customCompilerDist = layout.buildDirectory.dir("customCompiler_$version").get().dir("kotlin-native-prebuilt-macos-aarch64-${version.rawVersion}")
        systemProperty("kotlin.internal.native.test.compat.customCompilerDist", customCompilerDist.asFile.absolutePath)
        val konanLibDir = customCompilerDist.dir("konan").dir("lib")
        val runtimeJars = listOf("kotlin-native-compiler-embeddable.jar", "trove4j.jar")
        systemProperty(
            "kotlin.internal.native.test.compat.customCompilerClasspath",
            runtimeJars.map { konanLibDir.file(it).asFile.absolutePath }.joinToString(File.pathSeparator)
        )
        systemProperty("kotlin.internal.native.test.compat.customCompilerVersion", version.rawVersion)
        val nativeHome = layout.projectDirectory.dir("..").dir("..").dir("..").dir("kotlin-native").dir("dist")
        systemProperty("kotlin.internal.native.test.compat.currentCompilerDist", nativeHome.asFile.absolutePath)
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

customFirstStageTest("1.9.24")
customFirstStageTest("2.0.0")
customFirstStageTest("2.1.0")
customFirstStageTest("2.2.0")
customFirstStageTest("2.3.0-RC")

customSecondStageTest("2.2.0")
customSecondStageTest("2.3.0-RC")

projectTests {
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateNativeKlibCompatibilityTestsKt", generateTestsInBuildDirectory = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
    testData(project(":compiler").isolated, "testData/codegen/box")
    testData(project(":compiler").isolated, "testData/codegen/boxInline")
    testData(project(":native:native.tests").isolated, "testData/codegen/box")
}
