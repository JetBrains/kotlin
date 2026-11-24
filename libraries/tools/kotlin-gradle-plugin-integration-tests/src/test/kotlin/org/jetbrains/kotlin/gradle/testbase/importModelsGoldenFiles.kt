/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.Any
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import org.jetbrains.kotlin.importmodels.proto.version
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel

private val GOLDEN_FILES_ROOT = Path.of("src/test/resources/importModels")

private fun importModelsDumpRoot(projectPath: Path): Path = projectPath.resolve("build/kotlin/import-models")

/**
 * Compares the models the dump wrote for [projectName] with the golden files at [goldenRoot],
 * the checked-in ones by default: every model with a golden file must be dumped, with its
 * `.bin` beside its `.txt` and its text the printed form of its bytes, and the dump must hold
 * nothing without a golden file. All mismatches are reported in one failure, and the
 * `-Pkotlin.test.update.test.data=true` flag regenerates the golden files instead of failing.
 */
internal fun assertExpectedImportModelsPresentInDump(
    projectPath: Path,
    projectName: String,
    goldenRoot: Path = GOLDEN_FILES_ROOT / projectName,
) {
    val dumpRoot = importModelsDumpRoot(projectPath)
    // listDirectoryEntries fails on a missing directory, so an update run cannot pass without a dump
    val dumpEntryNames = dumpRoot.listDirectoryEntries().map { it.name }.sorted()
    val dumpedModelIds = dumpEntryNames
        .filter { it.endsWith(".txt") || it.endsWith(".bin") }
        .map { it.substringBeforeLast('.') }
        .distinct()
        .sorted()
    // A golden file's name is the model id, so the golden files are the expected model set
    val goldenModelIds = if (goldenRoot.isDirectory()) importModelIds(goldenRoot) else emptyList()
    // Gradle prints canonical paths, so the placeholder targets the project's real path
    val projectDir = projectPath.toRealPath().toString()
    val mismatches = buildList {
        if (dumpedModelIds.isEmpty() && goldenModelIds.isEmpty()) {
            add("Nothing to assert: no models in the dump and no golden files")
        }
        for (fileName in dumpEntryNames.filter { !it.endsWith(".txt") && !it.endsWith(".bin") }) {
            add("Unexpected file '$fileName' in the dump")
        }
        for (modelId in goldenModelIds) {
            if (modelId !in dumpedModelIds) {
                add("Model '$modelId' has a golden file, but the dump does not contain it: delete the golden file if the model is gone")
            }
        }
        for (modelId in dumpedModelIds) {
            if (!updateTestData && modelId !in goldenModelIds) {
                add("Model '$modelId' is in the dump but has no golden file: run with -Pkotlin.test.update.test.data=true to generate it")
            }
            val txtFile = dumpRoot / "$modelId.txt"
            val binFile = dumpRoot / "$modelId.bin"
            if (!txtFile.isRegularFile()) add("The dump has no '$modelId.txt'")
            if (!binFile.isRegularFile()) add("The dump has no '$modelId.bin' beside '$modelId.txt'")
            // An incomplete pair produces no golden file on an update run
            if (!txtFile.isRegularFile() || !binFile.isRegularFile()) continue
            val fromBin = try {
                Result.parseFrom(binFile.readBytes())
            } catch (e: IOException) {
                add("The binary leg of '$modelId' does not parse: ${e.message}")
                continue
            }
            val dumpedText = txtFile.readText().removeSuffix("\n")
            val printedText = ImportModelsTextFormat.printResult(fromBin).removeSuffix("\n")
            if (printedText != dumpedText) {
                add("The text leg of '$modelId' is not the printed form of its binary leg")
            }
            val actual = normalizeImportModelsResultText(fromBin, projectDir)
                .trimTrailingWhitespacesAndAddNewlineAtEOF()
            val goldenFile = goldenRoot / "$modelId.txt"
            if (updateTestData) {
                goldenFile.parent.createDirectories()
                goldenFile.writeText(actual)
            } else if (modelId in goldenModelIds) {
                val expected = goldenFile.readText().trimTrailingWhitespacesAndAddNewlineAtEOF()
                if (expected != actual) {
                    add("Dumped model '$modelId' differs from its golden file $goldenFile")
                }
            }
        }
    }
    if (mismatches.isNotEmpty()) throw AssertionError(mismatches.joinToString("\n"))
}

private fun importModelIds(directory: Path): List<String> =
    directory.listDirectoryEntries("*.txt").map { it.name.removeSuffix(".txt") }.sorted()

private val updateTestData: Boolean
    get() = System.getProperty("kotlin.test.update.test.data")?.toBoolean() == true

/**
 * Compares the normalized TextFormat of a `Result` served through the Tooling API with the
 * checked-in golden file for [projectName]. No update mode: the dump task owns the goldens.
 */
internal fun assertImportModelsGoldenResult(
    resultBytes: ByteArray,
    projectPath: Path,
    projectName: String,
    goldenFileName: String,
) {
    val actual = normalizeImportModelsResultText(
        Result.parseFrom(resultBytes),
        projectPath.toRealPath().toString(),
    ).trimTrailingWhitespacesAndAddNewlineAtEOF()
    assertMatchesGoldenFile(
        GOLDEN_FILES_ROOT / projectName / goldenFileName,
        actual,
        "add the model to KotlinImportModelsDumpIT and run that test with -Pkotlin.test.update.test.data=true",
    )
}

private fun assertMatchesGoldenFile(goldenFile: Path, actual: String, missingGoldenHint: String) {
    if (!goldenFile.isRegularFile()) {
        throw AssertionError(
            "Missing golden file $goldenFile: $missingGoldenHint",
        )
    }
    assertEquals(goldenFile.readText().trimTrailingWhitespacesAndAddNewlineAtEOF(), actual)
}

/**
 * Normalizes a dumped `Result` for comparison as readable TextFormat: the KGP version becomes a
 * sentinel, set-like fields sort, and the project's absolute path becomes a placeholder;
 * order-significant content is left as the dump wrote it.
 */
internal fun normalizeImportModelsResultText(result: Result, projectDir: String): String {
    // The dir is replaced in its escaped form and only as a whole path component,
    // so escaped paths still match and a longer path containing the dir keeps its text
    val printedDir = Regex.escape(printedPath(projectDir))
    return ImportModelsTextFormat.printResult(normalizeResult(result))
        .replace(Regex("(?<![^\"\\s])$printedDir(?=[\"/\\\\])"), "<PROJECT_DIR>")
}

/**
 * Returns the string as the TextFormat printer writes it, escaped, by printing it as an
 * error message and reading the printed value back out.
 */
private fun printedPath(value: String): String {
    val printed = ImportModelsTextFormat.printResult(
        resultModel { error = importModelError { errorMessage = value } },
    )
    return printed.substringAfter("error_message: \"").substringBeforeLast('"')
}

private fun normalizeResult(result: Result): Result = when {
    result.resultCase != Result.ResultCase.MODEL -> result
    result.model.`is`(BaseModel::class.java) -> resultModel {
        model = Any.pack(normalizeBaseModel(result.model.unpack(BaseModel::class.java)))
    }
    result.model.`is`(ProjectModel::class.java) -> resultModel {
        model = Any.pack(normalizeProjectModel(result.model.unpack(ProjectModel::class.java)))
    }
    else -> result
}

private fun normalizeBaseModel(model: BaseModel): BaseModel {
    val builder = BaseModel.newBuilder(model)
        .clearCapabilities()
        .addAllCapabilities(model.capabilitiesList.sorted())
    // A dropped plugin version stays dropped
    if (model.hasPluginVersion()) builder.pluginVersion = version { classifier = "KGP_VERSION" }
    return builder.build()
}

private fun normalizeProjectModel(model: ProjectModel): ProjectModel = ProjectModel.newBuilder(model)
    .clearCompilationUnitIds()
    .addAllCompilationUnitIds(model.compilationUnitIdsList.sortedBy { it.value })
    .build()
