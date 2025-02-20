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
import org.junit.rules.TemporaryFolder
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
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
    )

    @Serializable
    class Variant(
        val name: String,
        val attributes: Map<String, String>,
        val dependencies: List<Component>,
        val files: List<StubVariantFile> = emptyList(),
    )

    @Serializable
    class StubVariantFile(
        val url: String,
        val name: String = url,
        val size: Int = 0,
    ) {
        constructor(
            artifactId: String,
            version: String,
            extension: String,
            classifier: String? = null,
        ) : this(listOfNotNull(artifactId, version, classifier).joinToString("-") + ".${extension}")
    }

    @Serializable
    class Dependency(
        val group: String,
        val module: String,
        val version: Version,
        val attributes: Map<String, String>? = null,
    )

    @Serializable
    class Version(
        val requires: String,
    )
}

class MavenComponent(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val packaging: String,
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
                componentRoot.resolve(file.url).createNewFile()
            }
        }
    }

    fun addMavenComponent(mavenComponent: MavenComponent) {
        val componentRoot = createComponentRoot(mavenComponent)
        componentRoot.resolve("${mavenComponent.artifactId}-${mavenComponent.version}.pom").writeText(
            generatePom(mavenComponent)
        )
        componentRoot.resolve("${mavenComponent.artifactId}-${mavenComponent.version}.jar").createNewFile()
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
                appendChild(
                    doc.createElement("packaging").apply {
                        textContent = component.packaging
                    }
                )
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
    }
}