package org.jetbrains.dokka.base.transformers.pages.annotations

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SinceKotlinTransformer(val context: DokkaContext) : DocumentableTransformer {

    override fun invoke(original: DModule, context: DokkaContext) = original.transform() as DModule

    private fun <T : Documentable> T.transform(): Documentable =
        when (this) {
            is DModule -> copy(
                packages = packages.map { it.transform() as DPackage }
            )
            is DPackage -> copy(
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty }
            )
            is DClass -> copy(
                documentation = appendSinceKotlin(),
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty }
            )
            is DEnum -> copy(
                documentation = appendSinceKotlin(),
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty }
            )
            is DInterface -> copy(
                documentation = appendSinceKotlin(),
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty }
            )
            is DObject -> copy(
                documentation = appendSinceKotlin(),
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty }
            )
            is DAnnotation -> copy(
                documentation = appendSinceKotlin(),
                classlikes = classlikes.map { it.transform() as DClasslike },
                functions = functions.map { it.transform() as DFunction },
                properties = properties.map { it.transform() as DProperty }
            )
            is DFunction -> copy(
                documentation = appendSinceKotlin()
            )
            is DProperty -> copy(
                documentation = appendSinceKotlin()
            )
            is DParameter -> copy(
                documentation = appendSinceKotlin()
            )
            else -> this.also { context.logger.warn("Unrecognized documentable $this while SinceKotlin transformation") }
        }

    private fun Documentable.appendSinceKotlin() =
        sourceSets.fold(documentation) { acc, sourceSet ->
            safeAs<WithExtraProperties<Documentable>>()?.extra?.get(Annotations)?.content?.get(sourceSet)?.find {
                it.dri == DRI("kotlin", "SinceKotlin")
            }?.params?.get("version").safeAs<StringValue>()?.value?.let { version ->
                acc.mapValues {
                    if (it.key == sourceSet) it.value.copy(
                        it.value.children + listOf(
                            CustomTagWrapper(Text(version.dropWhile { it == '"' }.dropLastWhile { it == '"' }), "Since Kotlin")
                        )
                    ) else it.value
                }
            } ?: acc
        }
}