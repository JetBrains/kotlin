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
                Regex(it.matchingRegex).matches(packageName)
            }?.skipDeprecated
                    ?: globalOptions.skipDeprecated

            return !(condition && this.isDeprecated())
        }

        fun processModule(original: DModule) =
            filterPackages(original.packages).let { (modified, packages) ->
                if (!modified) original
                else
                    original.copy(
                        packages = packages
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
                val typeAliases = filterTypeAliases(pckg.typealiases).let { (listModified, list) ->
                    modified = modified || listModified
                    list
                }
                when {
                    !modified -> pckg
                    else -> {
                        packagesListChanged = true
                        pckg.copy(
                            functions = functions,
                            properties = properties,
                            classlikes = classlikes,
                            typealiases = typeAliases
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
                entry.copy(
                    functions = filterFunctions(entry.functions).second,
                    properties = filterProperties(entry.properties).second,
                    classlikes = filterClasslikes(entry.classlikes).second,
                )
            }

        private fun filterTypeAliases(typeAliases: List<DTypeAlias>) =
            typeAliases.filter { it.isAllowedInPackage() }.let {
                Pair(typeAliases.size != it.size, it)
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
                    is DClass -> classlike.copy(
                        constructors = filterFunctions(classlike.constructors).let {
                            modified = modified || it.first; it.second
                        },
                        functions = filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        properties = filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        classlikes = filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        }
                    )
                    is DAnnotation -> classlike.copy(
                        functions = filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        properties = filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        classlikes = filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        constructors = filterFunctions(classlike.constructors).let {
                            modified = modified || it.first; it.second
                        }
                    )
                    is DEnum -> classlike.copy(
                        entries = filterEnumEntries(classlike.entries),
                        functions = filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        properties = filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        classlikes = filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        },
                        constructors = filterFunctions(classlike.constructors).let {
                            modified = modified || it.first; it.second
                        },
                    )
                    is DInterface -> classlike.copy(
                        functions = filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        properties = filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        classlikes = filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        }
                    )
                    is DObject -> classlike.copy(
                        functions = filterFunctions(classlike.functions).let {
                            modified = modified || it.first; it.second
                        },
                        properties = filterProperties(classlike.properties).let {
                            modified = modified || it.first; it.second
                        },
                        classlikes = filterClasslikes(classlike.classlikes).let {
                            modified = modified || it.first; it.second
                        }
                    )
                }
                helper()
            }.let {
                Pair(it.size != classlikeList.size || modified, it)
            }
        }
    }
}
