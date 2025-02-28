/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.UklibModule
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization.serializeToZipArchive
import org.junit.rules.TemporaryFolder
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
    // Gradle can't handle explicit nulls
    explicitNulls = false
}

class GradleComponent(
    val module: GradleMetadataComponent,
    val pom: MavenComponent,
)

@Serializable
class GradleMetadataComponent(
    val formatVersion: String = "1.1",
    val component: Component,
    val createdBy: Map<String, Map<String, String>> = mapOf(
        "gradle" to mapOf(
            "version" to "8.11.1"
        )
    ),
    val variants: List<Variant>
) {
    @Serializable
    class Component(
        val group: String,
        val module: String,
        val version: String,
        val attributes: Map<String, String> = mapOf(
            "org.gradle.status" to "release"
        ),
    ) {
        val requiresDependency: Dependency = Dependency(group, module, Version(version))
    }

    @Serializable
    class Dependency(
        val group: String,
        val module: String,
        val version: Version,
        val attributes: Map<String, String>? = null,
    )

    @Serializable
    class Variant(
        val name: String,
        val attributes: Map<String, String>,
        val dependencies: List<Dependency>,
        val files: List<MockVariantFile> = emptyList(),
    )

    sealed class MockVariantType {
        object EmptyJar : MockVariantType()
        object MetadataJar : MockVariantType()
        internal data class UklibArchive(val fragments: (temporaryDirectory: File) -> Set<UklibFragment>) : MockVariantType()
    }

    @Serializable
    class MockVariantFile(
        val url: String,
        val name: String = url,
        val size: Int = 0,
        @kotlinx.serialization.Transient
        val type: MockVariantType = MockVariantType.EmptyJar,
    ) {
        constructor(
            artifactId: String,
            version: String,
            extension: String,
            classifier: String? = null,
            type: MockVariantType = MockVariantType.EmptyJar,
        ) : this(
            listOfNotNull(artifactId, version, classifier).joinToString("-") + ".${extension}",
            type = type,
        )
    }

    @Serializable
    class Version(
        val requires: String,
    )
}

class MavenComponent(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val packaging: String?,
    val dependencies: List<Dependency>,
    val gradleMetadataMarker: Boolean,
) {
    class Dependency(
        val groupId: String?,
        val artifactId: String?,
        val version: String?,
        val scope: String?,
    )
}

fun generateMockRepository(
    temporaryFolder: TemporaryFolder,
    gradleComponents: List<GradleComponent> = emptyList(),
    mavenComponents: List<MavenComponent> = emptyList(),
): File {
    val repositoryRoot = temporaryFolder.newFolder()
    val repository = MockRepository(repositoryRoot)
    gradleComponents.forEach {
        repository.addGradleComponent(
            it.module, it.pom
        )
    }
    mavenComponents.forEach {
        repository.addMavenComponent(it)
    }
    return repositoryRoot
}

private class MockRepository(
    private val root: File
) {
    fun addGradleComponent(
        gradleComponent: GradleMetadataComponent,
        mavenComponent: MavenComponent,
    ) {
        val componentRoot = createComponentRoot(mavenComponent)
        componentRoot.resolve("${mavenComponent.artifactId}-${mavenComponent.version}.pom").writeText(
            generatePom(mavenComponent)
        )
        componentRoot.resolve("${mavenComponent.artifactId}-${mavenComponent.version}.module").writeText(
            with(json.encodeToJsonElement(gradleComponent)) {
                // Gradle requires that "formatVersion" field be the first in the module json
                json.encodeToString(
                    jsonObject.entries
                        .sortedBy { (key, _) -> if (key == "formatVersion") 0 else 1 }
                        .map { it.key to it.value }
                        .toMap()
                )
            }
        )
        gradleComponent.variants.forEach { variant ->
            variant.files.forEach { file ->
                val mockFile = componentRoot.resolve(file.url)
                if (mockFile.exists()) {
                    error("Trying to overwrite mock file: $mockFile")
                }
                when (file.type) {
                    GradleMetadataComponent.MockVariantType.EmptyJar -> ZipOutputStream(FileOutputStream(mockFile)).use {  }
                    GradleMetadataComponent.MockVariantType.MetadataJar -> ZipOutputStream(FileOutputStream(mockFile)).use {
                        it.putNextEntry(ZipEntry("META-INF/kotlin-project-structure-metadata.json"))
                        it.closeEntry()
                    }
                    is GradleMetadataComponent.MockVariantType.UklibArchive -> {
                        val temporaryDirectory = root.resolve("tmp")
                        Uklib(
                            module = UklibModule(
                                fragments = file.type.fragments(temporaryDirectory)
                            ),
                            manifestVersion = Uklib.MAXIMUM_COMPATIBLE_UMANIFEST_VERSION,
                        ).serializeToZipArchive(
                            mockFile,
                            temporaryDirectory,
                        )
                    }
                }
            }
        }
    }

    fun addMavenComponent(mavenComponent: MavenComponent) {
        val componentRoot = createComponentRoot(mavenComponent)
        componentRoot.resolve("${mavenComponent.artifactId}-${mavenComponent.version}.pom").writeText(
            generatePom(mavenComponent)
        )
        componentRoot.resolve("${mavenComponent.artifactId}-${mavenComponent.version}.jar").also {
            // Avoid overwriting Gradle variant
            if (!it.exists()) {
                ZipOutputStream(FileOutputStream(it)).use {}
            }
        }
    }

    private fun createComponentRoot(component: MavenComponent): File {
        val path = component.groupId.split(".") + listOf(
            component.artifactId,
            component.version,
        )
        val componentRoot = root.resolve(path.joinToString("/"))
        componentRoot.mkdirs()
        return componentRoot
    }

    private fun generatePom(component: MavenComponent): String {
        val doc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

        doc.appendChild(
            doc.createElement("project").apply {
                setAttribute("xmlns", "http://maven.apache.org/POM/4.0.0")
                setAttribute("xsi:schemaLocation", "http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd")
                setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                if (component.gradleMetadataMarker) {
                    appendChild(
                        doc.createComment("do_not_remove: published-with-gradle-metadata")
                    )
                }
                appendChild(
                    doc.createElement("modelVersion").apply {
                        textContent = "4.0.0"
                    }
                )
                appendChild(
                    doc.createElement("groupId").apply {
                        textContent = component.groupId
                    }
                )
                appendChild(
                    doc.createElement("artifactId").apply {
                        textContent = component.artifactId
                    }
                )
                appendChild(
                    doc.createElement("version").apply {
                        textContent = component.version
                    }
                )
                component.packaging?.let {
                    appendChild(
                        doc.createElement("packaging").apply {
                            textContent = it
                        }
                    )
                }
                appendChild(
                    doc.createElement("dependencies").apply {
                        component.dependencies.forEach { dependency ->
                            appendChild(
                                doc.createElement("dependency").apply {
                                    dependency.groupId?.let {
                                        appendChild(
                                            doc.createElement("groupId").apply {
                                                textContent = it
                                            }
                                        )
                                    }
                                    dependency.artifactId?.let {
                                        appendChild(
                                            doc.createElement("artifactId").apply {
                                                textContent = it
                                            }
                                        )
                                    }
                                    dependency.version?.let {
                                        appendChild(
                                            doc.createElement("version").apply {
                                                textContent = it
                                            }
                                        )
                                    }
                                    dependency.scope?.let {
                                        appendChild(
                                            doc.createElement("scope").apply {
                                                textContent = it
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                )
            }
        )

        return ByteArrayOutputStream().apply {
            TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            }.transform(DOMSource(doc), StreamResult(this))
        }.toString()
            // Suppress \r\n newlines on Windows
            .replace("\r", "")
    }
}