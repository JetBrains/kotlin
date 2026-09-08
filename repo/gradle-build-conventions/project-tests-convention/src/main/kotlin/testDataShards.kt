/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

/**
 * Registers one [Test] task per testdata directory under [roots] and returns an aggregate task which
 * depends on all of them.
 *
 * This is the build side of the test generator's `testClassPerDirectory` mode: the generator emits one
 * small test class per testdata directory and mirrors the directory structure onto the package name,
 * so a directory maps onto exactly one generated class, and a task can be given inputs as narrow as
 * the directory itself:
 *
 * ```
 * directory  compiler/testData/diagnostics/tests/inference/pcla
 * class      <generatedPackage>/tests/inference/pcla/PhasedJvmDiagnosticLightTreeTestGenerated.class
 * task       shardTestsInferencePcla
 * ```
 *
 * A task is registered for every directory of the tree, including the ones the generator produced no
 * class for: an unmatched class file pattern makes a test task `NO-SOURCE`, so over-approximating
 * here is free, and it saves the build from having to repeat the generator's rules about which
 * testdata files and directories are taken into account.
 *
 * Note that [Test.getCandidateClassFiles] is derived from the task's own `include`/`exclude`
 * patterns, so the pattern set below both selects the tests to run and narrows the class files the
 * task declares as its input. It does *not* narrow [Test.getClasspath], which is annotated with
 * `@Classpath` and contains the single test classes directory holding the classes generated for all
 * the directories at once. Unless those are excluded from classpath normalization, a change in any
 * one of them invalidates every task registered here.
 *
 * @param generatedPackage the package the test generator puts the generated classes into, not
 *   including the testdata root directory name it appends itself.
 * @param roots testdata directories the generated classes were generated from.
 */
fun ProjectTestsExtension.testDataShards(
    generatedPackage: String,
    roots: List<Directory>,
    aggregateTaskName: String = "shardedTest",
    javaLauncher: JdkMajorVersion = DEFAULT_JAVA_LAUNCHER_FOR_TESTS,
    maxHeapSize: Size = testDefaultMaxHeapSize,
    minHeapSize: Size = testDefaultMinHeapSize,
    maxMetaspaceSize: Size = testDefaultMaxMetaspaceSize,
    reservedCodeCacheSize: Size = testDefaultReservedCodeCacheSize,
    garbageCollector: GarbageCollector? = testDefaultGC,
    defineJDKEnvVariables: List<JdkMajorVersion> = emptyList(),
    body: Test.() -> Unit = {},
): TaskProvider<Task> {
    val generatedPackagePath = generatedPackage.replace('.', '/')
    val repositoryRoot = project.rootDir
    val shardTasks = mutableListOf<TaskProvider<out Task>>()
    val directoriesByTaskName = mutableMapOf<String, File>()

    fun shardTask(taskName: String, configure: Test.() -> Unit) {
        shardTasks += testTask(
            taskName = taskName,
            javaLauncher = javaLauncher,
            maxHeapSize = maxHeapSize,
            minHeapSize = minHeapSize,
            maxMetaspaceSize = maxMetaspaceSize,
            reservedCodeCacheSize = reservedCodeCacheSize,
            garbageCollector = garbageCollector,
            defineJDKEnvVariables = defineJDKEnvVariables,
            skipInLocalBuild = false,
        ) {
            configure()
            body()
        }
    }

    for (root in roots) {
        val rootDirectory = root.asFile
        require(rootDirectory.isDirectory) { "Testdata root is not a directory: $rootDirectory" }

        rootDirectory.forEachDirectoryRecursively { directory ->
            val packagePath = (listOf(rootDirectory.name) + directory.relativeSegmentsTo(rootDirectory))
                .map { directoryNameToPackageSegment(it) }
            val taskName = packagePath.joinToString(separator = "", prefix = "shard") {
                it.replaceFirstChar(Char::uppercaseChar)
            }
            directoriesByTaskName.put(taskName, directory)?.let { clashing ->
                error("Test task name '$taskName' is derived from both '$clashing' and '$directory'")
            }

            val testDataDirectory = directory.toRelativeString(repositoryRoot).invariantSeparators()
            shardTask(taskName) {
                description = "Runs the tests generated for the '$testDataDirectory' testdata directory"
                // A single `*` doesn't cross directory boundaries, so this picks up the class of this
                // directory only; nested directories are covered by their own tasks.
                include("$generatedPackagePath/${packagePath.joinToString("/")}/*.class")
                // Same reasoning for the testdata: only the files lying directly in the directory.
                val ownTestData = project.fileTree(directory)
                ownTestData.include("*")
                testDataInputs().files.setFrom(ownTestData)
                // The files above are snapshotted with `@PathSensitive(RELATIVE)` relative to the
                // directory, i.e. by name only, which two directories can easily agree on. Make the
                // identity of the directory itself a part of the cache key as well.
                inputs.property("testDataDirectory", testDataDirectory)
            }
        }
    }

    // Tests which are written by hand rather than generated per testdata directory, so that the
    // aggregate task below runs everything the project's plain `test` task would.
    shardTask("shardOtherTests") {
        description = "Runs the tests of the project which are not generated per testdata directory"
        exclude("$generatedPackagePath/**")
    }

    return project.tasks.register(aggregateTaskName) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs all the tests of the project as one task per testdata directory"
        dependsOn(shardTasks)
    }
}

private fun File.forEachDirectoryRecursively(action: (File) -> Unit) {
    action(this)
    listFiles().orEmpty()
        .filter { it.isDirectory }
        .sortedBy { it.name }
        .forEach { it.forEachDirectoryRecursively(action) }
}

private fun File.relativeSegmentsTo(base: File): List<String> =
    toRelativeString(base).split(File.separatorChar).filter { it.isNotEmpty() }

private fun String.invariantSeparators(): String = replace(File.separatorChar, '/')

/**
 * Converts a testdata directory name into a single Java package segment.
 *
 * NB: this repeats `TestGeneratorUtil.directoryNameToPackageSegment`, which the test generator uses
 * to lay out the generated classes. The two have to agree, otherwise a generated class ends up in a
 * package no task selects and its tests silently stop running. Keep them in sync; the equivalence is
 * covered by comparing the tests executed by the aggregate task against the plain `test` task.
 */
private fun directoryNameToPackageSegment(directoryName: String): String {
    val escaped = buildString {
        for (character in directoryName) {
            append(if (Character.isJavaIdentifierPart(character)) character else '_')
        }
    }
    return when {
        escaped in JAVA_KEYWORDS -> "${escaped}_"
        escaped.first().isDigit() -> "_$escaped"
        else -> escaped
    }
}

private val JAVA_KEYWORDS = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
    "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
    "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
    "volatile", "while", "true", "false", "null", "_",
)
