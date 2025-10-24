/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.uklibs

import com.android.build.gradle.LibraryExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.KmpGradlePublicationMetadataIT.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class, ExperimentalWasmDsl::class)
@MppGradlePluginTests
@DisplayName("Smoke test Gradle publication metadata")
@OsCondition(enabledOnCI = [OS.LINUX]) // FIXME: KT-76778
class KmpGradlePublicationMetadataIT : KGPBaseTest() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Serializable
    data class GradleMetadata(
        val variants: Set<Variant>,
    )

    @Serializable
    data class Variant(
        val name: String,
        val attributes: Map<String, String>,
        @SerialName("available-at")
        val availableAt: ComponentPointer? = null,
        val files: List<VariantFile> = emptyList(),
    )

    @Serializable
    data class ComponentPointer(
        val url: String,
    )

    @Serializable
    data class VariantFile(
        val name: String,
        val url: String,
    )

    // FIXME: Test standard publication with Android - KT-76700

    @GradleTest
    fun `standard kmp publication`(version: GradleVersion) {
        val producer = kmpProducer(
            version,
            withJvm = true,
        ).publish()
        assertEquals(
            GradleMetadata(
                variants = rootVariantsSharedByAllPublications + standardKmpPublicationMetadataVariants + jvmSubcomponentVariants,
            ).prettyPrinted,
            producer.rootComponent.gradleMetadata.inputStream().use {
                json.decodeFromStream<GradleMetadata>(it).prettyPrinted
            }
        )
    }

    @GradleTest
    fun `kmp publication with uklibs - with jvm target`(version: GradleVersion) {
        val producer = kmpProducer(
            version,
            withJvm = true,
        ) {
            project.setUklibPublicationStrategy()
        }.publish()
        assertEquals(
            GradleMetadata(
                variants = rootVariantsSharedByAllPublications + uklibCompatibilityMetadataVariants + uklibVariants + uklibJvmVariants,
            ).prettyPrinted,
            producer.rootComponent.gradleMetadata.inputStream().use { input ->
                json.decodeFromStream<GradleMetadata>(input).prettyPrinted
            }
        )
    }

    @GradleTest
    fun `kmp publication with uklibs - with stub jvm target`(version: GradleVersion) {
        val producer = kmpProducer(
            version,
            withJvm = false,
        ) {
            project.setUklibPublicationStrategy()
        }.publish()
        assertEquals(
            GradleMetadata(
                variants = rootVariantsSharedByAllPublications + uklibCompatibilityMetadataVariants + uklibVariants + uklibJvmStubVariants,
            ).prettyPrinted,
            producer.rootComponent.gradleMetadata.inputStream().use { input ->
                json.decodeFromStream<GradleMetadata>(input).prettyPrinted
            }
        )
    }

    @GradleAndroidTest
    @AndroidTestVersions(
        minVersion = TestVersions.AGP.AGP_88
    )
    fun `kmp publication with uklibs - with stub jvm target - with KMP android library target`(
        version: GradleVersion,
        androidVersion: String,
    ) {
        val producer = kmpProducer(
            version,
            withJvm = false,
            androidVersion = androidVersion,
        ) {
            project.setUklibPublicationStrategy()
            project.plugins.apply("com.android.kotlin.multiplatform.library")
            project.applyMultiplatform {
                val target = targets.getByName("android")
                val klass = target::class.java.classLoader.loadClass("com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension")
                val compileSdk = klass.getMethod("setCompileSdk", Int::class.javaObjectType)
                compileSdk.invoke(target, 31)
                val namespace = klass.getMethod("setNamespace", String::class.java)
                namespace.invoke(target, "foo")
            }
        }.publish()
        assertEquals(
            GradleMetadata(
                variants = rootVariantsSharedByAllPublications
                        + uklibCompatibilityMetadataVariants
                        + uklibVariants
                        + uklibJvmStubVariants
                        + kmpAndroidLibraryVariants,
            ).prettyPrinted,
            producer.rootComponent.gradleMetadata.inputStream().use { input ->
                json.decodeFromStream<GradleMetadata>(input).prettyPrinted
            }
        )
    }

    @GradleAndroidTest
    fun `kmp publication with uklibs - with stub jvm target - with legacy android library target`(
        version: GradleVersion,
        androidVersion: String,
    ) {
        val producer = kmpProducer(
            version,
            withJvm = false,
            androidVersion = androidVersion,
        ) {
            project.setUklibPublicationStrategy()
            project.plugins.apply("com.android.library")
            (project.extensions.getByName("android") as LibraryExtension).apply {
                compileSdk = 31
                namespace = "foo"
            }
            project.applyMultiplatform {
                @Suppress("DEPRECATION")
                androidTarget {
                    publishLibraryVariants("debug", "release")
                }
            }
        }.publish()
        assertEquals(
            GradleMetadata(
                variants = rootVariantsSharedByAllPublications
                        + uklibCompatibilityMetadataVariants
                        + uklibVariants
                        + uklibJvmStubVariants
                        + androidLibraryDebugVariants
                        + androidLibraryReleaseVariants,
            ).prettyPrinted,
            producer.rootComponent.gradleMetadata.inputStream().use { input ->
                json.decodeFromStream<GradleMetadata>(input).prettyPrinted
            }
        )
    }

    private fun kmpProducer(
        version: GradleVersion,
        withJvm: Boolean,
        androidVersion: String? = null,
        configuration: GradleProjectBuildScriptInjectionContext.() -> Unit = {},
    ) = project("empty", version, buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion)).apply {
        if (androidVersion != null) {
            addAgpToBuildScriptCompilationClasspath(androidVersion)
        }
        addKgpToBuildScriptCompilationClasspath()
        buildScriptInjection {
            configuration()
            project.applyMultiplatform {
                iosArm64()
                iosX64()
                linuxArm64()
                linuxX64()
                if (withJvm) {
                    jvm()
                }
                wasmJs()
                wasmWasi()
                js()

                sourceSets.commonMain.get().compileSource("class Common")
            }
        }
    }
}

private val rootVariantsSharedByAllPublications = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.native.target" to "ios_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-iosarm64/1.0/empty-iosarm64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "iosArm64ApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.native.target" to "ios_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-iosarm64/1.0/empty-iosarm64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "iosArm64SourcesElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-metadata",
            "org.jetbrains.kotlin.native.target" to "ios_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-iosarm64/1.0/empty-iosarm64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "iosArm64MetadataElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.native.target" to "ios_x64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-iosx64/1.0/empty-iosx64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "iosX64ApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.native.target" to "ios_x64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-iosx64/1.0/empty-iosx64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "iosX64SourcesElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-metadata",
            "org.jetbrains.kotlin.native.target" to "ios_x64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-iosx64/1.0/empty-iosx64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "iosX64MetadataElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.js.compiler" to "ir",
            "org.jetbrains.kotlin.platform.type" to "js",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-js/1.0/empty-js-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "jsApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.js.compiler" to "ir",
            "org.jetbrains.kotlin.platform.type" to "js",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-js/1.0/empty-js-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "jsRuntimeElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.js.compiler" to "ir",
            "org.jetbrains.kotlin.platform.type" to "js",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-js/1.0/empty-js-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "jsSourcesElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.native.target" to "linux_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-linuxarm64/1.0/empty-linuxarm64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "linuxArm64ApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.native.target" to "linux_arm64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-linuxarm64/1.0/empty-linuxarm64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "linuxArm64SourcesElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.native.target" to "linux_x64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-linuxx64/1.0/empty-linuxx64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "linuxX64ApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.native.target" to "linux_x64",
            "org.jetbrains.kotlin.platform.type" to "native",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-linuxx64/1.0/empty-linuxx64-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "linuxX64SourcesElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.platform.type" to "wasm",
            "org.jetbrains.kotlin.wasm.target" to "js",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-wasm-js/1.0/empty-wasm-js-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "wasmJsApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.platform.type" to "wasm",
            "org.jetbrains.kotlin.wasm.target" to "js",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-wasm-js/1.0/empty-wasm-js-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "wasmJsRuntimeElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.platform.type" to "wasm",
            "org.jetbrains.kotlin.wasm.target" to "js",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-wasm-js/1.0/empty-wasm-js-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "wasmJsSourcesElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-api",
            "org.jetbrains.kotlin.platform.type" to "wasm",
            "org.jetbrains.kotlin.wasm.target" to "wasi",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-wasm-wasi/1.0/empty-wasm-wasi-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "wasmWasiApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.platform.type" to "wasm",
            "org.jetbrains.kotlin.wasm.target" to "wasi",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-wasm-wasi/1.0/empty-wasm-wasi-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "wasmWasiRuntimeElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.platform.type" to "wasm",
            "org.jetbrains.kotlin.wasm.target" to "wasi",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-wasm-wasi/1.0/empty-wasm-wasi-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "wasmWasiSourcesElements-published",
    ),
)

private val standardKmpPublicationMetadataVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-metadata",
            "org.jetbrains.kotlin.platform.type" to "common",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty-metadata-1.0.jar",
                url = "empty-1.0.jar",
            ),
        ),
        name = "metadataApiElements",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.platform.type" to "common",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty-kotlin-1.0-sources.jar",
                url = "empty-1.0-sources.jar",
            ),
        ),
        name = "metadataSourcesElements",
    ),
)

private val jvmSubcomponentVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "standard-jvm",
            "org.gradle.libraryelements" to "jar",
            "org.gradle.usage" to "java-api",
            "org.jetbrains.kotlin.platform.type" to "jvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-jvm/1.0/empty-jvm-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "jvmApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "standard-jvm",
            "org.gradle.libraryelements" to "jar",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "jvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-jvm/1.0/empty-jvm-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "jvmRuntimeElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "standard-jvm",
            "org.gradle.libraryelements" to "jar",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "jvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-jvm/1.0/empty-jvm-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "jvmSourcesElements-published",
    ),
)

private val uklibVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.usage" to "kotlin-uklib-runtime",
            "org.jetbrains.kotlin.uklib" to "true",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty.uklib",
                url = "empty-1.0.uklib",
            ),
        ),
        name = "uklibRuntimeElements",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.usage" to "kotlin-uklib-api",
            "org.jetbrains.kotlin.uklib" to "true",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty.uklib",
                url = "empty-1.0.uklib",
            ),
        ),
        name = "uklibApiElements",
    ),
)

private val uklibJvmStubVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.usage" to "java-api",
            "org.gradle.jvm.environment" to "standard-jvm",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty-1.0.jar",
                url = "empty-1.0.jar",
            ),
        ),
        name = "javaApiElements",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.usage" to "java-runtime",
            "org.gradle.jvm.environment" to "standard-jvm",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty-1.0.jar",
                url = "empty-1.0.jar",
            ),
        ),
        name = "javaRuntimeElements",
    ),
)

private val uklibJvmVariants = jvmSubcomponentVariants

private val uklibCompatibilityMetadataVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-metadata",
            "org.jetbrains.kotlin.platform.type" to "common",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty-metadata-1.0-psm.jar",
                url = "empty-1.0-psm.jar",
            ),
        ),
        name = "metadataApiElements",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "non-jvm",
            "org.gradle.usage" to "kotlin-runtime",
            "org.jetbrains.kotlin.platform.type" to "common",
        ),
        availableAt = null,
        files = mutableListOf(
            VariantFile(
                name = "empty-kotlin-1.0-sources.jar",
                url = "empty-1.0-metadata-sources.jar",
            ),
        ),
        name = "metadataSourcesElements",
    ),
)

private val kmpAndroidLibraryVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.libraryelements" to "aar",
            "org.gradle.usage" to "java-api",
            "org.jetbrains.kotlin.platform.type" to "jvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android/1.0/empty-android-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "androidApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.libraryelements" to "aar",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "jvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android/1.0/empty-android-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "androidRuntimeElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.libraryelements" to "jar",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "jvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android/1.0/empty-android-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "androidSourcesElements-published",
    ),
)

private val androidLibraryDebugVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "com.android.build.api.attributes.BuildTypeAttr" to "debug",
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.usage" to "java-api",
            "org.jetbrains.kotlin.platform.type" to "androidJvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android-debug/1.0/empty-android-debug-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "debugApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "com.android.build.api.attributes.BuildTypeAttr" to "debug",
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "androidJvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android-debug/1.0/empty-android-debug-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "debugRuntimeElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "com.android.build.api.attributes.BuildTypeAttr" to "debug",
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.libraryelements" to "jar",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "androidJvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android-debug/1.0/empty-android-debug-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "debugSourcesElements-published",
    ),
)

private val androidLibraryReleaseVariants = mutableSetOf(
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.usage" to "java-api",
            "org.jetbrains.kotlin.platform.type" to "androidJvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android/1.0/empty-android-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "releaseApiElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "library",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "androidJvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android/1.0/empty-android-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "releaseRuntimeElements-published",
    ),
    Variant(
        attributes = mutableMapOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.libraryelements" to "jar",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "androidJvm",
        ),
        availableAt = ComponentPointer(
            url = "../../empty-android/1.0/empty-android-1.0.module",
        ),
        files = mutableListOf(
        ),
        name = "releaseSourcesElements-published",
    ),
)