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
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel

internal val GOLDEN_FILES_ROOT = Path.of("src/test/resources/importModels")

/**
 * Returns the path of the serialized `Result` bytes checked in beside the golden file [golden].
 */
internal fun resultBytesFile(golden: Path): Path =
    golden.resolveSibling(golden.fileName.toString().removeSuffix(".txt") + ".bin")

/**
 * Compares the normalized TextFormat of a dumped import model with the checked-in golden file for [projectName],
 * regenerating the golden file with the `-Pkotlin.test.update.test.data=true` flag instead of failing.
 */
internal fun assertImportModelsGolden(projectPath: Path, projectName: String, modelId: String) {
    // Gradle prints canonical paths, so the placeholder targets the project's real path
    val resultBytes = projectPath.resolve("build/kotlin-import-models/$modelId.bin").readBytes()
    val dumpedResult = Result.parseFrom(resultBytes)
    val actual = normalizeImportModelsResultText(dumpedResult, projectPath.toRealPath().toString())
        .trimTrailingWhitespacesAndAddNewlineAtEOF()
    val goldenFile = GOLDEN_FILES_ROOT / projectName / "$modelId.txt"
    if (System.getProperty("kotlin.test.update.test.data")?.toBoolean() == true) {
        goldenFile.parent.createDirectories()
        goldenFile.writeText(actual)
        // The serialized Result bytes beside the golden file are the fixture for IDEA-side tests that do not run Gradle
        resultBytesFile(goldenFile).writeBytes(resultBytes)
        return
    }
    assertMatchesGoldenFile(
        goldenFile,
        actual,
        "run with -Pkotlin.test.update.test.data=true to generate it",
    )
}

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

/**
 * Asserts the serialized `Result` bytes checked in beside a golden file hold the golden file's
 * model, type URL included. The model comparison runs both sides through [normalizeResult],
 * so the fixture's embedded plugin version and the order of set-like fields are not content;
 * every other difference fails.
 */
internal fun assertFixtureMatchesGolden(golden: Path, bytesFile: Path) {
    val result = Result.parseFrom(bytesFile.readBytes())
    assertEquals(
        Result.ResultCase.MODEL,
        result.resultCase,
        "The fixture beside the golden file $golden holds no model",
    )
    val goldenResult = ImportModelsTextFormat.parseResult(golden.readText())
    assertEquals(
        goldenResult.model.typeUrl,
        result.model.typeUrl,
        "The fixture beside the golden file $golden holds a different type URL",
    )
    assertEquals(
        ImportModelsTextFormat.printResult(normalizeResult(goldenResult)),
        ImportModelsTextFormat.printResult(normalizeResult(result)),
        "The fixture beside the golden file $golden holds a different model",
    )
}

private fun assertMatchesGoldenFile(goldenFile: Path, actual: String, missingGoldenHint: String) {
    if (!goldenFile.isRegularFile()) {
        throw AssertionError(
            "Missing golden file $goldenFile: $missingGoldenHint",
        )
    }
    // An IDE save that adds or strips the golden's final newline cannot fail the comparison
    assertEquals(goldenFile.readText().trimTrailingWhitespacesAndAddNewlineAtEOF(), actual)
}

/**
 * Normalizes a dumped `Result` for comparison as readable TextFormat: the KGP version becomes a
 * sentinel, set-like fields sort, and the project's absolute path becomes a placeholder;
 * order-significant content is left as the dump wrote it.
 */
internal fun normalizeImportModelsResultText(result: Result, projectDir: String): String {
    // The path is replaced in its printed (escaped) form: a path with escaped characters,
    // such as a Windows path's doubled backslashes, no longer appears raw
    return ImportModelsTextFormat.printResult(normalizeResult(result))
        .replace(printedPath(projectDir), "<PROJECT_DIR>")
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

private fun normalizeBaseModel(model: BaseModel): BaseModel = BaseModel.newBuilder(model)
    .clearCapabilities()
    .addAllCapabilities(model.capabilitiesList.sorted())
    .setPluginVersion(version { classifier = "KGP_VERSION" })
    .build()

private fun normalizeProjectModel(model: ProjectModel): ProjectModel = ProjectModel.newBuilder(model)
    .clearCompilationUnitIds()
    .addAllCompilationUnitIds(model.compilationUnitIdsList.sortedBy { it.value })
    .build()
