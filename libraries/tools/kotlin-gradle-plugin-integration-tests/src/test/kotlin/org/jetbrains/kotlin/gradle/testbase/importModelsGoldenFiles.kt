/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import org.jetbrains.kotlin.importmodels.proto.version
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel

private val GOLDEN_FILES_ROOT = Path.of("src/test/resources/importModels")
private const val TYPE_URL_PREFIX = "type.googleapis.com/"

/**
 * Compares the normalized ProtoJSON of a dumped import model with the checked-in golden file for [projectName],
 * regenerating the golden file with the `-Pkotlin.test.update.test.data=true` flag instead of failing.
 */
internal fun assertImportModelsGolden(projectPath: Path, projectName: String, dumpFileName: String) {
    // Gradle prints canonical paths, so the placeholder targets the project's real path
    val actual = normalizeImportModelsResultJson(
        projectPath.resolve("build/kotlin-import-models/$dumpFileName").readText(),
        projectPath.toRealPath().toString(),
    ).trimTrailingWhitespacesAndAddNewlineAtEOF()
    val goldenFile = GOLDEN_FILES_ROOT / projectName / dumpFileName
    if (System.getProperty("kotlin.test.update.test.data")?.toBoolean() == true) {
        goldenFile.parent.createDirectories()
        goldenFile.writeText(actual)
        return
    }
    if (!goldenFile.isRegularFile()) {
        throw AssertionError(
            "Missing golden file $goldenFile: run with -Pkotlin.test.update.test.data=true to generate it",
        )
    }
    // An IDE save that adds or strips the golden's final newline cannot fail the comparison
    assertEquals(goldenFile.readText().trimTrailingWhitespacesAndAddNewlineAtEOF(), actual)
}

/**
 * Normalizes a dumped `Result` ProtoJSON for comparison: the KGP version becomes a sentinel, set-like
 * fields sort, and the project's absolute path becomes a placeholder; order-significant content is
 * left as the dump wrote it.
 */
internal fun normalizeImportModelsResultJson(json: String, projectDir: String): String {
    val result = KotlinImportModelSerialization.parseResult(json)
    // The path is replaced in its printed (JSON-escaped) form: a path with escaped
    // characters, such as a Windows path's doubled backslashes, no longer appears raw
    return KotlinImportModelSerialization.toJson(normalizeResult(result))
        .replace(jsonPrinted(projectDir), "<PROJECT_DIR>")
}

/**
 * Returns the string as the ProtoJSON printer writes it, escaped, by printing it as an
 * error message and reading the printed value back out.
 */
private fun jsonPrinted(value: String): String {
    val printed = KotlinImportModelSerialization.toJson(
        resultModel { error = importModelError { errorMessage = value } },
    )
    return printed.substringAfter("\"error_message\": \"").substringBeforeLast('"')
}

private fun normalizeResult(result: Result): Result = when {
    result.resultCase != Result.ResultCase.MODEL -> result
    result.model.typeUrl == TYPE_URL_PREFIX + BaseModel.getDescriptor().fullName ->
        Result.parseFrom(
            KotlinImportModelSerialization.modelResult(normalizeBaseModel(result.model.unpack(BaseModel::class.java))),
        )
    result.model.typeUrl == TYPE_URL_PREFIX + ProjectModel.getDescriptor().fullName ->
        Result.parseFrom(
            KotlinImportModelSerialization.modelResult(normalizeProjectModel(result.model.unpack(ProjectModel::class.java))),
        )
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
