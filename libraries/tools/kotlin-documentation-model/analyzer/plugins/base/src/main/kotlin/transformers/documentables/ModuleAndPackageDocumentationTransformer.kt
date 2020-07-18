package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.nio.file.Files
import java.nio.file.Paths


internal class ModuleAndPackageDocumentationTransformer(
    private val context: DokkaContext,
    private val kotlinAnalysis: KotlinAnalysis
) : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>): List<DModule> {

        val modulesAndPackagesDocumentation =
            context.configuration.sourceSets
                .map { sourceSet ->
                    Pair(sourceSet.moduleDisplayName, sourceSet) to
                            sourceSet.includes.map { it.toPath() }
                                .also {
                                    it.forEach {
                                        if (Files.notExists(it))
                                            context.logger.warn("Not found file under this path ${it.toAbsolutePath()}")
                                    }
                                }
                                .filter { Files.exists(it) }
                                .flatMap {
                                    it.toFile()
                                        .readText()
                                        .split(Regex("(\n|^)# (?=(Module|Package))")) // Matches heading with Module/Package to split by
                                        .filter { it.isNotEmpty() }
                                        .map {
                                            it.split(
                                                Regex(" "),
                                                2
                                            )
                                        } // Matches space between Module/Package and fully qualified name
                                }.groupBy({ it[0] }, {
                                    it[1].split(Regex("\n"), 2) // Matches new line after fully qualified name
                                        .let { it[0].trim() to it[1].trim() }
                                }).mapValues {
                                    it.value.toMap()
                                }
                }.toMap()

        return modules.map { module ->

            val moduleDocumentation =
                module.sourceSets.mapNotNull { pd ->
                    val doc = modulesAndPackagesDocumentation[Pair(module.name, pd)]
                    val facade = kotlinAnalysis[pd].facade
                    try {
                        doc?.get("Module")?.get(module.name)?.run {
                            pd to MarkdownParser(
                                facade,
                                facade.moduleDescriptor.getPackage(FqName.topLevel(Name.identifier(""))),
                                context.logger
                            ).parse(this)
                        }
                    } catch (e: IllegalArgumentException) {
                        context.logger.error(e.message.orEmpty())
                        null
                    }
                }.toMap()

            val packagesDocumentation = module.packages.map {
                it.name to it.sourceSets.mapNotNull { pd ->
                    val doc = modulesAndPackagesDocumentation[Pair(module.name, pd)]
                    val facade = kotlinAnalysis[pd].facade
                    val descriptor = facade.moduleDescriptor.getPackage(FqName(it.name.let { if(it == "[JS root]") "" else it }))
                    doc?.get("Package")?.get(it.name)?.run {
                        pd to MarkdownParser(
                            facade,
                            descriptor,
                            context.logger
                        ).parse(this)
                    }
                }.toMap()
            }.toMap()

            module.copy(
                documentation = mergeDocumentation(module.documentation, moduleDocumentation),
                packages = module.packages.map {
                    val packageDocumentation = packagesDocumentation[it.name]
                    if (packageDocumentation != null && packageDocumentation.isNotEmpty())
                        it.copy(documentation = mergeDocumentation(it.documentation, packageDocumentation))
                    else
                        it
                }
            )
        }
    }

    private fun mergeDocumentation(origin: Map<DokkaSourceSet, DocumentationNode>, new: Map<DokkaSourceSet, DocumentationNode>) =
        (origin.asSequence() + new.asSequence())
            .distinct()
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> DocumentationNode(values.flatMap { it.children }) }

}
