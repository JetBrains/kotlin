/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization.errorResult
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

/**
 * Producer of the import models of one Gradle project, independent of any transport:
 * [enumerateRequests] lists the requests the project serves, and [serveRequest] answers one request,
 * taking the model id and optional parameter bytes and returning the serialized `Result` bytes.
 * A request the project cannot answer becomes an error `Result`, never a thrown exception.
 */
internal class KotlinImportModelProducer(private val project: Project) {

    /**
     * Lists the requests this project serves; the set varies by project.
     * Consumers could take the list instead of hardcoding the model catalog,
     * as the dump does, so later models need no consumer change.
     */
    fun enumerateRequests(): List<Request> = buildList {
        add(Request(KotlinImportModelIds.BASE))
        if (servesJvmProjectInformation()) add(Request(KotlinImportModelIds.PROJECT_INFORMATION))
    }

    /**
     * Answers one request with the serialized `Result` bytes: [modelId] selects the model, and
     * [parameters] carries the request's serialized parameters, null when the model takes none.
     * A null or unknown [modelId] answers [Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID].
     * A known id this project does not serve answers [Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_ID].
     * Parameters for a parameterless model answer [Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS].
     * A failure inside production answers [Error.Type.ERROR_TYPE_INTERNAL_ERROR] with the failure's message.
     * Errors, such as OOM, propagate by design.
     */
    fun serveRequest(modelId: String?, parameters: ByteArray?): ByteArray = try {
        when (modelId) {
            null -> errorResult(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID, "Kotlin import model ID is required")
            KotlinImportModelIds.BASE -> serveParameterless(modelId, parameters) {
                KotlinImportModelSerialization.modelResult(baseInformation())
            }
            KotlinImportModelIds.PROJECT_INFORMATION -> if (servesJvmProjectInformation()) {
                serveParameterless(modelId, parameters) {
                    KotlinImportModelSerialization.modelResult(projectInformation())
                }
            } else {
                errorResult(
                    Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_ID,
                    "Kotlin import model '$modelId' is not supported by this project",
                )
            }
            else -> errorResult(
                Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID,
                "Kotlin import model '$modelId' is not known to the producer",
            )
        }
    } catch (failure: Exception) { // Errors (OOM, StackOverflowError) propagate by design
        errorResult(
            Error.Type.ERROR_TYPE_INTERNAL_ERROR,
            "Failed to produce Kotlin import model '$modelId': ${failure.message ?: failure.toString()}",
        )
    }

    private fun baseInformation(): BaseModel = baseModel {
        id = KotlinImportModelIds.BASE
        pluginVersion = project.kotlinToolingVersion.toImportModelVersion()
        capabilities += appliedCapabilities()
    }

    private fun projectInformation(): ProjectModel {
        val target = project.kotlinJvmExtension.target
        // The plain JVM target's name is empty, so the id scheme carries "jvm" as its target key
        val targetKey = target.targetName.ifEmpty { "jvm" }
        val buildPath = project.currentBuildId().buildPath
        val projectPath = project.path
        val unitIds = target.compilations.toList().map { compilation ->
            compilationUnitId {
                value = compilationUnitIdValue(
                    buildPath = buildPath,
                    projectPath = projectPath,
                    targetKey = targetKey,
                    compilationName = compilation.name,
                )
            }
        }
        return projectModel {
            id = KotlinImportModelIds.PROJECT_INFORMATION
            // Ordered for byte stability; the contract does not require it
            compilationUnitIds += unitIds.sortedBy { it.value }
        }
    }

    // The strict accessor: the producer exists only on projects with a Kotlin plugin applied,
    // so a missing `kotlin` extension is a broken state that serveRequest answers with INTERNAL_ERROR
    private fun appliedCapabilities(): List<BaseModel.Capability> = when (project.kotlinExtension) {
        is KotlinJvmProjectExtension -> listOf(BaseModel.Capability.CAPABILITY_KOTLIN_JVM)
        else -> emptyList()
    }.sorted()

    private fun servesJvmProjectInformation(): Boolean = project.kotlinExtensionOrNull is KotlinJvmProjectExtension

    private fun serveParameterless(
        modelId: String,
        parameters: ByteArray?,
        produce: () -> ByteArray,
    ): ByteArray = if (parameters == null) produce() else errorResult(
        Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS,
        "Kotlin import model '$modelId' does not accept parameters",
    )

    /**
     * One request the producer serves: the model id and, for parameterized models, its serialized parameters.
     */
    class Request(val modelId: String, val parameters: ByteArray? = null)
}

/**
 * The stable opaque key of a compilation unit: <build-path>|<project-path>|<target-key>|<compilation-name>,
 * with `%` and `|` percent-escaped per component.
 * Consumers match unit ids across builds, so the layout and the escaping must not change.
 */
internal fun compilationUnitIdValue(
    buildPath: String,
    projectPath: String,
    targetKey: String,
    compilationName: String,
): String = listOf(buildPath, projectPath, targetKey, compilationName)
    .joinToString("|") { it.replace("%", "%25").replace("|", "%7C") }

private fun KotlinToolingVersion.toImportModelVersion(): Version = version {
    major = this@toImportModelVersion.major
    minor = this@toImportModelVersion.minor
    patch = this@toImportModelVersion.patch
    this@toImportModelVersion.classifier?.let { classifier = it }
}
