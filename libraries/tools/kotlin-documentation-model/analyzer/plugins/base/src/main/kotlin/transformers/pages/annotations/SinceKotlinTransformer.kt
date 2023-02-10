package org.jetbrains.dokka.base.transformers.pages.annotations

import com.intellij.util.containers.ComparatorUtil.max
import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.annotations
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.utilities.associateWithNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SinceKotlinVersion constructor(str: String) : Comparable<SinceKotlinVersion> {
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
}

class SinceKotlinTransformer(val context: DokkaContext) : DocumentableTransformer {

    private val minSinceKotlinVersionOfPlatform = mapOf(
        Platform.common to SinceKotlinVersion("1.0"),
        Platform.jvm to SinceKotlinVersion("1.0"),
        Platform.js to SinceKotlinVersion("1.1"),
        Platform.native to SinceKotlinVersion("1.3"),
        Platform.wasm to SinceKotlinVersion("1.8"),
    )

    override fun invoke(original: DModule, context: DokkaContext) = original.transform() as DModule

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
                ?.params?.get("version").safeAs<StringValue>()?.value
                ?.let { SinceKotlinVersion(it) }

        val minSinceKotlin = minSinceKotlinVersionOfPlatform[sourceSet.analysisPlatform]
            ?: throw IllegalStateException("No value for platform: ${sourceSet.analysisPlatform}")

        return annotatedVersion?.takeIf { version -> version >= minSinceKotlin } ?: minSinceKotlin
    }


    private fun Documentable.calculateVersions(parent: SourceSetDependent<SinceKotlinVersion>?): SourceSetDependent<SinceKotlinVersion> {
        return sourceSets.associateWithNotNull { sourceSet ->
            val version = getVersion(sourceSet)
            val parentVersion = parent?.get(sourceSet)
            if (parentVersion != null)
                max(version, parentVersion)
            else
                version
        }
    }

    private fun Documentable.appendSinceKotlin(versions: SourceSetDependent<SinceKotlinVersion>) =
        sourceSets.fold(documentation) { acc, sourceSet ->

            val version = versions[sourceSet]

            val sinceKotlinCustomTag = CustomTagWrapper(
                CustomDocTag(
                    listOf(
                        Text(
                            version.toString()
                        )
                    ),
                    name = MarkdownElementTypes.MARKDOWN_FILE.name
                ),
                "Since Kotlin"
            )
            if (acc[sourceSet] == null)
                acc + (sourceSet to DocumentationNode(listOf(sinceKotlinCustomTag)))
            else
                acc.mapValues {
                    if (it.key == sourceSet) it.value.copy(
                        it.value.children + listOf(
                            sinceKotlinCustomTag
                        )
                    ) else it.value
                }
        }

    internal companion object {
        internal const val SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP = "dokka.shouldDisplaySinceKotlin"
        internal fun shouldDisplaySinceKotlin() =
            System.getProperty(SHOULD_DISPLAY_SINCE_KOTLIN_SYS_PROP) in listOf("true", "1")
    }
}
