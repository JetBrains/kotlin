/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages.annotations


import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.annotations
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.utilities.associateWithNotNull

public class SinceKotlinVersion(str: String) : Comparable<SinceKotlinVersion> {
    private val parts: List<Int> = str.split(".").map { it.toInt() }

    /**
     * Corner case: 1.0 == 1.0.0
     */
    override fun compareTo(other: SinceKotlinVersion): Int {
        val i1 = parts.listIterator()
        val i2 = other.parts.listIterator()

        while (i1.hasNext() || i2.hasNext()) {
            val diff = (if (i1.hasNext()) i1.next() else 0) - (if (i2.hasNext()) i2.next() else 0)
            if (diff != 0) return diff
        }

        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    internal companion object {
        internal const val SINCE_KOTLIN_TAG_NAME = "Since Kotlin"

        private val minVersionOfPlatform = mapOf(
            Platform.common to SinceKotlinVersion("1.0"),
            Platform.jvm to SinceKotlinVersion("1.0"),
            Platform.js to SinceKotlinVersion("1.1"),
            Platform.native to SinceKotlinVersion("1.3"),
            Platform.wasm to SinceKotlinVersion("1.8"),
        )

        fun minVersionOfPlatform(platform: Platform): SinceKotlinVersion {
            return minVersionOfPlatform[platform]
                ?: throw IllegalStateException("No value for platform: $platform")
        }

        /**
         * Should be in sync with [extractSinceKotlinVersionFromCustomTag]
         */
        fun createCustomTagFromSinceKotlinVersion(
            version: SinceKotlinVersion?,
            platform: Platform
        ): CustomTagWrapper {
            val sinceKotlinVersion = version?: minVersionOfPlatform(platform)
            return CustomTagWrapper(
                CustomDocTag(
                    children = listOf(Text(sinceKotlinVersion.toString())),
                    name = MARKDOWN_ELEMENT_FILE_NAME
                ),
                SINCE_KOTLIN_TAG_NAME
            )
        }

        /**
         * Should be in sync with [createCustomTagFromSinceKotlinVersion]
         */
        fun extractSinceKotlinVersionFromCustomTag(
            tagWrapper: CustomTagWrapper,
            platform: Platform
        ): SinceKotlinVersion {
            val customTag = tagWrapper.root as? CustomDocTag
            val sinceKotlinVersionText = customTag?.children?.firstOrNull() as? Text
            val sinceKotlinVersion = sinceKotlinVersionText?.body?.let(::SinceKotlinVersion)
            return sinceKotlinVersion ?: minVersionOfPlatform(platform)
        }

    }
}

public class SinceKotlinTransformer(
    public val context: DokkaContext
) : DocumentableTransformer {

    override fun invoke(original: DModule, context: DokkaContext): DModule = original.transform() as DModule

    private fun <T : Documentable> T.transform(parent: SourceSetDependent<SinceKotlinVersion>? = null): Documentable {
        val versions = calculateVersions(parent)
        return when (this) {
            is DModule -> copy(
                packages = packages.map { it.transform() as DPackage }
            )

            is DPackage -> copy(
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty },
                typealiases = typealiases.map { it.transform() as DTypeAlias }
            )

            is DClass -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DEnum -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DInterface -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DObject -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DTypeAlias -> copy(
                documentation = appendSinceKotlin(versions)
            )

            is DAnnotation -> copy(
                documentation = appendSinceKotlin(versions),
                classlikes = classlikes.map { it.transform(versions) as DClasslike },
                functions = functions.map { it.transform(versions) as DFunction },
                properties = properties.map { it.transform(versions) as DProperty }
            )

            is DFunction -> copy(
                documentation = appendSinceKotlin(versions)
            )

            is DProperty -> copy(
                documentation = appendSinceKotlin(versions)
            )

            is DParameter -> copy(
                documentation = appendSinceKotlin(versions)
            )

            else -> this.also { context.logger.warn("Unrecognized documentable $this while SinceKotlin transformation") }
        }
    }

    private fun List<Annotations.Annotation>.findSinceKotlinAnnotation(): Annotations.Annotation? =
        this.find { it.dri.packageName == "kotlin" && it.dri.classNames == "SinceKotlin" }

    private fun Documentable.getVersion(sourceSet: DokkaConfiguration.DokkaSourceSet): SinceKotlinVersion {
        val annotatedVersion =
            annotations()[sourceSet]
                ?.findSinceKotlinAnnotation()
                ?.params?.let { it["version"] as? StringValue }?.value
                ?.let { SinceKotlinVersion(it) }

        val minSinceKotlin = SinceKotlinVersion.minVersionOfPlatform(sourceSet.analysisPlatform)

        return annotatedVersion?.takeIf { version -> version >= minSinceKotlin } ?: minSinceKotlin
    }


    private fun Documentable.calculateVersions(parent: SourceSetDependent<SinceKotlinVersion>?): SourceSetDependent<SinceKotlinVersion> {
        return sourceSets.associateWithNotNull { sourceSet ->
            val version = getVersion(sourceSet)
            val parentVersion = parent?.get(sourceSet)
            if (parentVersion != null)
                maxOf(version, parentVersion)
            else
                version
        }
    }

    private fun Documentable.appendSinceKotlin(versions: SourceSetDependent<SinceKotlinVersion>) =
        sourceSets.fold(documentation) { acc, sourceSet ->

            val sinceKotlinCustomTag = SinceKotlinVersion.createCustomTagFromSinceKotlinVersion(
                version = versions[sourceSet],
                platform = sourceSet.analysisPlatform
            )
            if (acc[sourceSet] == null)
                acc + (sourceSet to DocumentationNode(listOf(sinceKotlinCustomTag)))
            else
                acc.mapValues {
                    if (it.key == sourceSet) it.value.copy(
                        it.value.children + listOf(sinceKotlinCustomTag)
                    ) else it.value
                }
        }
}
