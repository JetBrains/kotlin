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
import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent.Variant
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.setUklibResolutionStrategy
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream

val json = Json {
    encodeDefaults = true
    prettyPrint = true
}

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
        val size: Int = 0
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

class MockRepository(
    val root: File
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

class UklibResolutionTestsWithMockComponents {
    @get:Rule
    val tmpDir = TemporaryFolder()

    @Test
    fun `uklib resolution - transforms in platform configuration`() {
        val repo = MockRepository(tmpDir.newFolder())
        repo.addGradleComponent(
            GradleMetadataComponent(
                component = fooBarGradleComponent,
                variants = listOf(uklibVariant)
            ),
            fooBarUklibMavenComponent,
        )

        val consumer = uklibConsumer {
            kotlin {
                iosArm64()
                sourceSets.commonMain.dependencies { implementation("foo:bar:1.0") }
            }
            repositories.maven(repo.root)
        }

        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.iosArm64().compilationRes(),
        )
    }

    @Test
    fun `uklib resolution - consuming old variants in new`() {
        val repo = MockRepository(tmpDir.newFolder())
        repo.addGradleComponent(
            GradleMetadataComponent(
                component = fooBarGradleComponent,
                variants = listOf(metadataJarVariant, iosKlibVariant),
            ),
            fooBarUklibMavenComponent,
        )

        val consumer = uklibConsumer {
            kotlin {
                linuxArm64()
                sourceSets.commonMain.dependencies { implementation("foo:bar:1.0") }
            }
            repositories.maven(repo.root)
        }

        assertEqualsPP(
            mapOf(
                "foo:bar:1.0" to ResolvedComponentWithArtifacts(
                    configuration="uklibApiElements",
                    artifacts=mutableListOf(uklibVariantAttributes + uklibTransformationIosArm64Attributes + releaseStatus)
                ),
            ),
            consumer.multiplatformExtension.linuxArm64().compilationRes(),
        )
    }

    private fun uklibConsumer(code: Project.() -> Unit = {}): Project {
        return buildProjectWithMPP(
            preApplyCode = {
                fakeUklibTransforms()
                setUklibResolutionStrategy(KmpResolutionStrategy.ResolveUklibsAndResolvePSMLeniently)
                // Test stdlib in a separate test
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            },
            code = code,
        ).evaluate()
    }

    private val fooBarGradleComponent = GradleMetadataComponent.Component(
        group = "foo",
        module = "bar",
        version = "1.0",
    )
    private val fooBarUklibMavenComponent = MavenComponent(
        "foo", "bar", "1.0",
        packaging = "uklib",
        dependencies = listOf(),
        true,
    )

    private val uklibVariant = Variant(
        name = "uklibApiElements",
        attributes = mapOf(
            "org.gradle.usage" to "kotlin-uklib-api",
            "org.gradle.category" to "library",
        ),
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "uklib"
            )
        ),
        dependencies = listOf()
    )

    private val metadataJarVariant = Variant(
        name = "metadataApiElements",
        attributes = mapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-metadata",
            "org.jetbrains.kotlin.platform.type" to "common",
        ),
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "jar"
            )
        ),
        dependencies = listOf()
    )

    private val iosKlibVariant = Variant(
        name = "iosArm64ApiElements-published",
        attributes = mapOf(
            "artifactType" to "org.jetbrains.kotlin.klib", // wtf?
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.native.target" to "ios_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "klib"
            )
        ),
        dependencies = listOf()
    )

    private val iosArm64Variant = Variant(
        name = "iosArm64ApiElements-published",
        attributes = mapOf(
            "artifactType" to "org.jetbrains.kotlin.klib",
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.native.target" to "ios_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        files = listOf(
            GradleMetadataComponent.StubVariantFile(
                artifactId = "bar",
                version = "1.0",
                extension = "klib"
            )
        ),
        dependencies = listOf()
    )
}