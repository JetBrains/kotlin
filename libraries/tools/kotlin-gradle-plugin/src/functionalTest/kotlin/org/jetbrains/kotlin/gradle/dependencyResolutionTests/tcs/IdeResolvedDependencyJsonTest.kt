/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.KlibExtra
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.ide.IdeResolvedDependencyJson
import org.jetbrains.kotlin.gradle.plugin.ide.toJson
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasOf
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import org.jetbrains.kotlin.tooling.core.withValue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeResolvedDependencyJsonTest {

    private val root = File("/work/project").absoluteFile
    private val relativeToRoot: (File) -> String = { it.relativeTo(root).invariantSeparatorsPath }

    private val project = IdeaKotlinProjectCoordinates(
        buildName = "build",
        buildPath = ":",
        projectPath = ":lib",
        projectName = "lib",
    )

    private fun IdeaKotlinDependency.encode(): JsonElement =
        KgpJson.default.encodeToJsonElement(IdeResolvedDependencyJson.serializer(), toJson(relativeToRoot))

    @Test
    fun `resolved binary dependency`() {
        val noteKey = extrasKeyOf<String>("note")
        val filesKey = extrasKeyOf<List<File>>("files")
        val dependency = IdeaKotlinResolvedBinaryDependency(
            binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
            classpath = IdeaKotlinClasspath(root.resolve("libs/a.jar")),
            coordinates = IdeaKotlinBinaryCoordinates("org.example", "a", "1.0"),
            extras = mutableExtrasOf(noteKey withValue "hello", filesKey withValue listOf(root.resolve("x.txt"))),
        )

        assertEquals(
            buildJsonObject {
                put("kind", "resolvedBinary")
                put("binaryType", "KOTLIN_COMPILE")
                put("classpath", buildJsonArray { add(JsonPrimitive("libs/a.jar")) })
                put("coordinates", "org.example:a:1.0")
                put("extras", buildJsonObject {
                    put(noteKey.stableString, "hello")
                    put(filesKey.stableString, buildJsonArray { add(JsonPrimitive("x.txt")) })
                })
            },
            dependency.encode(),
        )
    }

    @Test
    fun `unresolved binary dependency`() {
        val dependency = IdeaKotlinUnresolvedBinaryDependency(
            cause = "Could not resolve",
            coordinates = IdeaKotlinBinaryCoordinates("org.example", "missing", null),
        )

        assertEquals(
            buildJsonObject {
                put("kind", "unresolvedBinary")
                put("cause", "Could not resolve")
                put("coordinates", "org.example:missing")
                put("extras", buildJsonObject { })
            },
            dependency.encode(),
        )
    }

    @Test
    fun `source and project artifact dependencies`() {
        val source = IdeaKotlinSourceDependency(
            type = IdeaKotlinSourceDependency.Type.DependsOn,
            coordinates = IdeaKotlinSourceCoordinates(project, "commonMain"),
        )
        val projectArtifact = IdeaKotlinProjectArtifactDependency(
            type = IdeaKotlinSourceDependency.Type.Regular,
            coordinates = project,
        )

        assertEquals(
            buildJsonObject {
                put("kind", "source")
                put("type", "DependsOn")
                put("coordinates", "${project}/commonMain")
                put("extras", buildJsonObject { })
            },
            source.encode(),
        )
        assertEquals(
            buildJsonObject {
                put("kind", "projectArtifact")
                put("type", "Regular")
                put("coordinates", project.toString())
                put("extras", buildJsonObject { })
            },
            projectArtifact.encode(),
        )
    }

    @Test
    fun `unresolved binary dependency without coordinates`() {
        val dependency = IdeaKotlinUnresolvedBinaryDependency(cause = null, coordinates = null)

        assertEquals(
            buildJsonObject {
                put("kind", "unresolvedBinary")
                put("extras", buildJsonObject { })
            },
            dependency.encode(),
        )
    }

    @Test
    fun `extras of known types keep their structure`() {
        val originKey = extrasKeyOf<IdeaKotlinDependency>("origin")
        val nestedKey = extrasKeyOf<Extras>("nested")
        val klibKey = extrasKeyOf<KlibExtra>("klib")
        val timeKey = extrasKeyOf<Double>("time")
        val filesKey = extrasKeyOf<Array<File>>("files")
        val dependency = IdeaKotlinSourceDependency(
            type = IdeaKotlinSourceDependency.Type.Regular,
            coordinates = IdeaKotlinSourceCoordinates(project, "commonMain"),
            extras = mutableExtrasOf(
                originKey withValue IdeaKotlinProjectArtifactDependency(IdeaKotlinSourceDependency.Type.Regular, project),
                nestedKey withValue extrasOf(extrasKeyOf<File>("file") withValue root.resolve("a/b.txt")),
                klibKey withValue KlibExtra("native", "lib", null, null, listOf("linux_x64"), null, null, false),
                timeKey withValue Double.NaN,
                filesKey withValue arrayOf(root.resolve("x.txt")),
            ),
        )

        assertEquals(
            buildJsonObject {
                put(originKey.stableString, buildJsonObject {
                    put("kind", "projectArtifact")
                    put("type", "Regular")
                    put("coordinates", project.toString())
                    put("extras", buildJsonObject { })
                })
                put(nestedKey.stableString, buildJsonObject { put(extrasKeyOf<File>("file").stableString, "a/b.txt") })
                put(klibKey.stableString, buildJsonObject {
                    put("builtInsPlatform", "native")
                    put("uniqueName", "lib")
                    put("nativeTargets", buildJsonArray { add(JsonPrimitive("linux_x64")) })
                    put("isInterop", false)
                })
                put(timeKey.stableString, "NaN")
                put(filesKey.stableString, buildJsonArray { add(JsonPrimitive("x.txt")) })
            },
            dependency.encode().jsonObject["extras"],
        )
    }
}
