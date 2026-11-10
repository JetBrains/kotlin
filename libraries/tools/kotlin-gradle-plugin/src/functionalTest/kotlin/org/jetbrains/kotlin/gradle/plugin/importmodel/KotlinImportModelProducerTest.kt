/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.UnknownDomainObjectException
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class KotlinImportModelProducerTest {
    @Test
    fun `serves base and project information for a Kotlin JVM project`() {
        val project = buildProjectWithJvm { }
        project.evaluate()
        val producer = KotlinImportModelProducer(project)

        assertEquals(
            listOf(KotlinImportModelIds.BASE, KotlinImportModelIds.PROJECT_INFORMATION),
            producer.enumerateRequests().map { it.modelId },
        )

        val base = producer.serveRequest(KotlinImportModelIds.BASE, null).baseModel()
        assertEquals(KotlinImportModelIds.BASE, base.id)
        assertEquals(project.kotlinToolingVersion.major, base.pluginVersion.major)
        assertEquals(project.kotlinToolingVersion.minor, base.pluginVersion.minor)
        assertEquals(project.kotlinToolingVersion.patch, base.pluginVersion.patch)
        // hasClassifier, not the getter: an unset field returns "" and would fail
        // against a release version's null classifier
        assertEquals(project.kotlinToolingVersion.classifier != null, base.pluginVersion.hasClassifier())
        project.kotlinToolingVersion.classifier?.let { assertEquals(it, base.pluginVersion.classifier) }
        assertEquals(listOf(BaseModel.Capability.CAPABILITY_KOTLIN_JVM), base.capabilitiesList)

        val projectModel = producer.serveRequest(KotlinImportModelIds.PROJECT_INFORMATION, null).projectModel()
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, projectModel.id)
        assertEquals(listOf(":|:|jvm|main", ":|:|jvm|test"), projectModel.compilationUnitIdsList.map { it.value })
    }

    @Test
    fun `lists every Kotlin compilation of the JVM target`() {
        val project = buildProjectWithJvm { kotlinJvmExtension.target.compilations.create("integrationTest") }
        project.evaluate()
        val producer = KotlinImportModelProducer(project)

        val projectModel = producer.serveRequest(KotlinImportModelIds.PROJECT_INFORMATION, null).projectModel()
        assertEquals(
            listOf(":|:|jvm|integrationTest", ":|:|jvm|main", ":|:|jvm|test"),
            projectModel.compilationUnitIdsList.map { it.value },
        )
    }

    @Test
    fun `serves the same request twice in one build with identical bytes`() {
        // Holds within one KGP version only: Protobuf serialization is not canonical across versions
        val project = buildProjectWithJvm { }
        project.evaluate()
        val producer = KotlinImportModelProducer(project)

        assertContentEquals(
            producer.serveRequest(KotlinImportModelIds.BASE, null),
            producer.serveRequest(KotlinImportModelIds.BASE, null),
        )
        assertContentEquals(
            producer.serveRequest(KotlinImportModelIds.PROJECT_INFORMATION, null),
            producer.serveRequest(KotlinImportModelIds.PROJECT_INFORMATION, null),
        )
    }

    @Test
    fun `answers unknown model ids with UNKNOWN_MODEL_ID`() {
        val project = buildProjectWithJvm { }
        project.evaluate()
        val producer = KotlinImportModelProducer(project)

        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID, producer.serveRequest("unknownModel", null).errorType())
        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID, producer.serveRequest(null, null).errorType())
    }

    @Test
    fun `answers a model the project does not serve with UNSUPPORTED_MODEL_ID`() {
        val project = buildProjectWithMPP { }
        project.evaluate()
        val producer = KotlinImportModelProducer(project)

        assertEquals(listOf(KotlinImportModelIds.BASE), producer.enumerateRequests().map { it.modelId })

        assertEquals(
            Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_ID,
            producer.serveRequest(KotlinImportModelIds.PROJECT_INFORMATION, null).errorType(),
        )
        // Multiplatform projects report no capability until their models are served by the later tickets
        assertEquals(
            emptyList<BaseModel.Capability>(),
            producer.serveRequest(KotlinImportModelIds.BASE, null).baseModel().capabilitiesList,
        )
    }

    @Test
    fun `answers a KMP project with a jvm target with UNSUPPORTED_MODEL_ID for projectInformation`() {
        // The gate is the Kotlin/JVM plugin's own extension: a KMP jvm target has Kotlin/JVM
        // compilations, but its per-target models are the KMP tickets' work
        val project = buildProjectWithMPP { kotlin { jvm() } }
        project.evaluate()
        val producer = KotlinImportModelProducer(project)

        assertEquals(listOf(KotlinImportModelIds.BASE), producer.enumerateRequests().map { it.modelId })
        assertEquals(
            Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_ID,
            producer.serveRequest(KotlinImportModelIds.PROJECT_INFORMATION, null).errorType(),
        )
    }

    @Test
    fun `answers parameters for a parameterless model with UNSUPPORTED_MODEL_PARAMS`() {
        val project = buildProjectWithJvm { }
        project.evaluate()
        val producer = KotlinImportModelProducer(project)

        assertEquals(
            Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS,
            producer.serveRequest(KotlinImportModelIds.BASE, byteArrayOf(0)).errorType(),
        )
    }

    @Test
    fun `answers an exception inside production with INTERNAL_ERROR and a message`() {
        val project = buildProject { } // no Kotlin plugin applied: the 'kotlin' extension is missing
        val producer = KotlinImportModelProducer(project)

        // The same missing-extension failure the producer runs into, captured for its exact wording
        val failure = assertFailsWith<UnknownDomainObjectException> { project.kotlinExtension }
        val error = Result.parseFrom(producer.serveRequest(KotlinImportModelIds.BASE, null)).error
        assertEquals(Error.Type.ERROR_TYPE_INTERNAL_ERROR, error.errorType)
        // Both the model id and the underlying failure's message are part of the error message
        assertEquals("Failed to produce Kotlin import model 'base': ${failure.message}", error.errorMessage)
    }

    @Test
    fun `escapes compilation unit id components without collisions`() {
        assertEquals(
            ":included%7Cbuild|:app%25demo|jvm|test%7Cfixture",
            compilationUnitIdValue(":included|build", ":app%demo", "jvm", "test|fixture"),
        )
        assertNotEquals(
            compilationUnitIdValue("a|b", "c", "d", "e"),
            compilationUnitIdValue("a", "b", "c", "d|e"),
        )
    }
}

private fun ByteArray.errorType(): Error.Type = Result.parseFrom(this).error.errorType

private fun ByteArray.baseModel(): BaseModel = Result.parseFrom(this).model.unpack(BaseModel::class.java)

private fun ByteArray.projectModel(): ProjectModel = Result.parseFrom(this).model.unpack(ProjectModel::class.java)
