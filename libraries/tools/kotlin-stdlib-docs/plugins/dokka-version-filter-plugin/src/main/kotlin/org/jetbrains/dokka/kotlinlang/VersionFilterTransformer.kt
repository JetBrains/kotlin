package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinVersion
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class VersionFilterTransformer(private val dokkaContext: DokkaContext) :
    DocumentableTransformer {

    private val targetVersion =
        configuration<VersionFilterPlugin, VersionFilterConfiguration>(dokkaContext)?.targetVersion?.let {
            SinceKotlinVersion(
                it
            )
        }

    override fun invoke(original: DModule, context: DokkaContext): DModule = original.transform() as DModule

    private fun <T : Documentable> T.transform(): Documentable? =
        when (this) {
            is DModule -> copy(
                packages = packages.mapNotNull { it.transform() as DPackage? }
            )

            is DPackage -> copy(
                classlikes = classlikes.mapNotNull { it.transform() as DClasslike? },
                functions = functions.mapNotNull { it.transform() as DFunction? },
                properties = properties.mapNotNull { it.transform() as DProperty? },
                typealiases = typealiases.mapNotNull { it.transform() as DTypeAlias? }
            ).notEmpty()

            is DClass -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                    classlikes = classlikes.mapNotNull { it.transform() as DClasslike? },
                    functions = functions.mapNotNull { it.transform() as DFunction? },
                    properties = properties.mapNotNull { it.transform() as DProperty? }
                )
            }

            is DEnum -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                    classlikes = classlikes.mapNotNull { it.transform() as DClasslike? },
                    functions = functions.mapNotNull { it.transform() as DFunction? },
                    properties = properties.mapNotNull { it.transform() as DProperty? }
                )
            }

            is DInterface -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                    classlikes = classlikes.mapNotNull { it.transform() as DClasslike? },
                    functions = functions.mapNotNull { it.transform() as DFunction? },
                    properties = properties.mapNotNull { it.transform() as DProperty? }
                )
            }

            is DObject -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                    classlikes = classlikes.mapNotNull { it.transform() as DClasslike? },
                    functions = functions.mapNotNull { it.transform() as DFunction? },
                    properties = properties.mapNotNull { it.transform() as DProperty? }
                )
            }

            is DTypeAlias -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                )
            }

            is DAnnotation -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                    classlikes = classlikes.mapNotNull { it.transform() as DClasslike? },
                    functions = functions.mapNotNull { it.transform() as DFunction? },
                    properties = properties.mapNotNull { it.transform() as DProperty? }
                )
            }

            is DFunction -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                )
            }

            is DProperty -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                )
            }

            is DParameter -> filterSourceSets().ifNotEmpty {
                this@transform.copy(
                    sourceSets = this,
                )
            }

            else -> this.also { dokkaContext.logger.warn("Unrecognized documentable $this while SinceKotlin transformation") }
        }

    private fun DPackage.notEmpty() =
        this.takeUnless { classlikes.isEmpty() && functions.isEmpty() && properties.isEmpty() }

    private fun Documentable.filterSourceSets(): Set<DokkaConfiguration.DokkaSourceSet> = this.sourceSets.filter {
        val currentVersion = getVersionFromCustomTag(it)
        targetVersion == null || currentVersion == null || currentVersion <= targetVersion
    }.toSet()


    private fun Documentable.getVersionFromCustomTag(sourceSet: DokkaConfiguration.DokkaSourceSet): SinceKotlinVersion? {
        return documentation[sourceSet]?.children?.find { it is CustomTagWrapper && it.name == "Since Kotlin" }
            ?.let { (it.children[0] as? Text)?.body?.let { txt -> SinceKotlinVersion(txt) } }
    }
}
