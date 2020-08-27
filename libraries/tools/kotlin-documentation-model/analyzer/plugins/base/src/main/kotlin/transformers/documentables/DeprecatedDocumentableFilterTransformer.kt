package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class DeprecatedDocumentableFilterTransformer(val context: DokkaContext) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>) = modules.map { original ->
        val sourceSet = original.sourceSets.single()
        val packageOptions =
            sourceSet.perPackageOptions
        original.let {
            DeprecatedDocumentableFilter(sourceSet, packageOptions).processModule(it)
        }
    }

    private class DeprecatedDocumentableFilter(
        val globalOptions: DokkaConfiguration.DokkaSourceSet,
        val packageOptions: List<DokkaConfiguration.PackageOptions>
    ) {

        fun <T> T.isAllowedInPackage(): Boolean where T : WithExtraProperties<T>, T : Documentable {
            val packageName = this.dri.packageName
            val condition = packageName != null && packageOptions.firstOrNull {
                packageName.startsWith(it.prefix)
            }?.skipDeprecated
                    ?: globalOptions.skipDeprecated

            return !(condition && this.isDeprecated())
        }

        fun processModule(original: DModule) =
            filterPackages(original.packages).let { (modified, packages) ->
                if (!modified) original
                else
                    DModule(
                        original.name,
                        packages = packages,
                        documentation = original.documentation,
                        sourceSets = original.sourceSets,
                        extra = original.extra
                    )
            }


        private fun filterPackages(packages: List<DPackage>): Pair<Boolean, List<DPackage>> {
            var packagesListChanged = false
            val filteredPackages = packages.mapNotNull { pckg ->
                var modified = false
                val functions = filterFunctions(pckg.functions).let { (listModified, list) ->
                    modified = modified || listModified
                    list
                }
                val properties = filterProperties(pckg.properties).let { (listModified, list) ->
                    modified = modified || listModified
                    list
                }
                val classlikes = filterClasslikes(pckg.classlikes).let { (listModified, list) ->
                    modified = modified || listModified
                    list
                }
                when {
                    !modified -> pckg
                    else -> {
                        packagesListChanged = true
                        DPackage(
                            pckg.dri,
                            functions,
                            properties,
                            classlikes,
                            pckg.typealiases,
                            pckg.documentation,
                            pckg.expectPresentInSet,
                            pckg.sourceSets,
                            pckg.extra
                        )
                    }
                }
            }
            return Pair(packagesListChanged, filteredPackages)
        }

        private fun filterFunctions(
            functions: List<DFunction>
        ) = functions.filter { it.isAllowedInPackage() }.let {
            Pair(it.size != functions.size, it)
        }

        private fun filterProperties(
            properties: List<DProperty>
        ): Pair<Boolean, List<DProperty>> = properties.filter {
            it.isAllowedInPackage()
        }.let {
            Pair(properties.size != it.size, it)
        }

        private fun filterEnumEntries(entries: List<DEnumEntry>) =
            entries.filter { it.isAllowedInPackage() }.map { entry ->
                DEnumEntry(
                    entry.dri,
                    entry.name,
                    entry.documentation,
                    entry.expectPresentInSet,
                    filterFunctions(entry.functions).second,
                    filterProperties(entry.properties).second,
                    filterClasslikes(entry.classlikes).second,
                    entry.sourceSets,
                    entry.extra
                )
            }

        private fun filterClasslikes(
            classlikeList: List<DClasslike>
        ): Pair<Boolean, List<DClasslike>> {
            var modified = false
            return classlikeList.filter { classlike ->
                when (classlike) {
                    is DClass -> classlike.isAllowedInPackage()
                    is DInterface -> classlike.isAllowedInPackage()
                    is DEnum -> classlike.isAllowedInPackage()
                    is DObject -> classlike.isAllowedInPackage()
                    is DAnnotation -> classlike.isAllowedInPackage()
                }
            }.map { classlike ->
                fun helper(): DClasslike = when (classlike) {
                    is DClass -> DClass(
                        classlike.dri,
                        classlike.name,
                        filterFunctions(classlike.constructors).let {
                            modified = modified || it.first; it.second
                        },
                        filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        classlike.sources,
                        classlike.visibility,
                        classlike.companion,
                        classlike.generics,
                        classlike.supertypes,
                        classlike.documentation,
                        classlike.expectPresentInSet,
                        classlike.modifier,
                        classlike.sourceSets,
                        classlike.extra
                    )
                    is DAnnotation -> DAnnotation(
                        classlike.name,
                        classlike.dri,
                        classlike.documentation,
                        classlike.expectPresentInSet,
                        classlike.sources,
                        filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        classlike.visibility,
                        classlike.companion,
                        filterFunctions(classlike.constructors).let {
                            modified = modified || it.first; it.second
                        },
                        classlike.generics,
                        classlike.sourceSets,
                        classlike.extra
                    )
                    is DEnum -> DEnum(
                        classlike.dri,
                        classlike.name,
                        filterEnumEntries(classlike.entries),
                        classlike.documentation,
                        classlike.expectPresentInSet,
                        classlike.sources,
                        filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        classlike.visibility,
                        classlike.companion,
                        filterFunctions(classlike.constructors).let {
                            modified = modified || it.first; it.second
                        },
                        classlike.supertypes,
                        classlike.sourceSets,
                        classlike.extra
                    )
                    is DInterface -> DInterface(
                        classlike.dri,
                        classlike.name,
                        classlike.documentation,
                        classlike.expectPresentInSet,
                        classlike.sources,
                        filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        classlike.visibility,
                        classlike.companion,
                        classlike.generics,
                        classlike.supertypes,
                        classlike.sourceSets,
                        classlike.extra
                    )
                    is DObject -> DObject(
                        classlike.name,
                        classlike.dri,
                        classlike.documentation,
                        classlike.expectPresentInSet,
                        classlike.sources,
                        filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        classlike.visibility,
                        classlike.supertypes,
                        classlike.sourceSets,
                        classlike.extra
                    )
                }
                helper()
            }.let {
                Pair(it.size != classlikeList.size || modified, it)
            }
        }
    }
}
