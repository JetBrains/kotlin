package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

abstract class SuppressedByConditionDocumentableFilterTransformer(val context: DokkaContext) :
    PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> =
        modules.map { module ->
            val (documentable, wasChanged) = processModule(module)
            documentable.takeIf { wasChanged } ?: module
        }

    abstract fun shouldBeSuppressed(d: Documentable): Boolean

    private fun processModule(module: DModule): DocumentableWithChanges<DModule> {
        val afterProcessing = module.packages.map { processPackage(it) }
        val processedModule = module.takeIf { afterProcessing.none { it.changed } }
            ?: module.copy(packages = afterProcessing.mapNotNull { it.documentable })
        return DocumentableWithChanges(processedModule, afterProcessing.any { it.changed })
    }

    private fun processPackage(dPackage: DPackage): DocumentableWithChanges<DPackage> {
        if (shouldBeSuppressed(dPackage)) return DocumentableWithChanges.filteredDocumentable()

        val classlikes = dPackage.classlikes.map { processClassLike(it) }
        val typeAliases = dPackage.typealiases.map { processMember(it) }
        val functions = dPackage.functions.map { processMember(it) }
        val properies = dPackage.properties.map { processProperty(it) }

        val wasChanged = (classlikes + typeAliases + functions + properies).any { it.changed }
        return (dPackage.takeIf { !wasChanged } ?: dPackage.copy(
            classlikes = classlikes.mapNotNull { it.documentable },
            typealiases = typeAliases.mapNotNull { it.documentable },
            functions = functions.mapNotNull { it.documentable },
            properties = properies.mapNotNull { it.documentable }
        )).let { processedPackage -> DocumentableWithChanges(processedPackage, wasChanged) }
    }

    private fun processClassLike(classlike: DClasslike): DocumentableWithChanges<DClasslike> {
        if (shouldBeSuppressed(classlike)) return DocumentableWithChanges.filteredDocumentable()

        val functions = classlike.functions.map { processMember(it) }
        val classlikes = classlike.classlikes.map { processClassLike(it) }
        val properties = classlike.properties.map { processProperty(it) }
        val companion = (classlike as? WithCompanion)?.companion?.let { processClassLike(it) }

        val wasClasslikeChanged = (functions + classlikes + properties).any { it.changed } || companion?.changed == true
        return when (classlike) {
            is DClass -> {
                val constructors = classlike.constructors.map { processMember(it) }
                val wasClassChange =
                    wasClasslikeChanged || constructors.any { it.changed }
                (classlike.takeIf { !wasClassChange } ?: classlike.copy(
                    functions = functions.mapNotNull { it.documentable },
                    classlikes = classlikes.mapNotNull { it.documentable },
                    properties = properties.mapNotNull { it.documentable },
                    constructors = constructors.mapNotNull { it.documentable },
                    companion = companion?.documentable as? DObject
                )).let { DocumentableWithChanges(it, wasClassChange) }
            }
            is DInterface -> (classlike.takeIf { !wasClasslikeChanged } ?: classlike.copy(
                functions = functions.mapNotNull { it.documentable },
                classlikes = classlikes.mapNotNull { it.documentable },
                properties = properties.mapNotNull { it.documentable },
                companion = companion?.documentable as? DObject
            )).let { DocumentableWithChanges(it, wasClasslikeChanged) }
            is DObject -> (classlike.takeIf { !wasClasslikeChanged } ?: classlike.copy(
                functions = functions.mapNotNull { it.documentable },
                classlikes = classlikes.mapNotNull { it.documentable },
                properties = properties.mapNotNull { it.documentable },
            )).let { DocumentableWithChanges(it, wasClasslikeChanged) }
            is DAnnotation -> {
                val constructors = classlike.constructors.map { processMember(it) }
                val wasClassChange =
                    wasClasslikeChanged || constructors.any { it.changed }
                (classlike.takeIf { !wasClassChange } ?: classlike.copy(
                    functions = functions.mapNotNull { it.documentable },
                    classlikes = classlikes.mapNotNull { it.documentable },
                    properties = properties.mapNotNull { it.documentable },
                    constructors = constructors.mapNotNull { it.documentable },
                    companion = companion?.documentable as? DObject
                )).let { DocumentableWithChanges(it, wasClassChange) }
            }
            is DEnum -> {
                val constructors = classlike.constructors.map { processMember(it) }
                val entries = classlike.entries.map { processEnumEntry(it) }
                val wasClassChange =
                    wasClasslikeChanged || (constructors + entries).any { it.changed }
                (classlike.takeIf { !wasClassChange } ?: classlike.copy(
                    functions = functions.mapNotNull { it.documentable },
                    classlikes = classlikes.mapNotNull { it.documentable },
                    properties = properties.mapNotNull { it.documentable },
                    constructors = constructors.mapNotNull { it.documentable },
                    companion = companion?.documentable as? DObject,
                    entries = entries.mapNotNull { it.documentable }
                )).let { DocumentableWithChanges(it, wasClassChange) }
            }
        }
    }

    private fun processEnumEntry(dEnumEntry: DEnumEntry): DocumentableWithChanges<DEnumEntry> {
        if (shouldBeSuppressed(dEnumEntry)) return DocumentableWithChanges.filteredDocumentable()

        val functions = dEnumEntry.functions.map { processMember(it) }
        val properties = dEnumEntry.properties.map { processProperty(it) }
        val classlikes = dEnumEntry.classlikes.map { processClassLike(it) }

        val wasChanged = (functions + properties + classlikes).any { it.changed }
        return (dEnumEntry.takeIf { !wasChanged } ?: dEnumEntry.copy(
            functions = functions.mapNotNull { it.documentable },
            classlikes = classlikes.mapNotNull { it.documentable },
            properties = properties.mapNotNull { it.documentable },
        )).let { DocumentableWithChanges(it, wasChanged) }
    }

    private fun processProperty(dProperty: DProperty): DocumentableWithChanges<DProperty> {
        if (shouldBeSuppressed(dProperty)) return DocumentableWithChanges.filteredDocumentable()

        val getter = dProperty.getter?.let { processMember(it) } ?: DocumentableWithChanges(null, false)
        val setter = dProperty.setter?.let { processMember(it) } ?: DocumentableWithChanges(null, false)

        val wasChanged = getter.changed || setter.changed
        return (dProperty.takeIf { !wasChanged } ?: dProperty.copy(
            getter = getter.documentable,
            setter = setter.documentable
        )).let { DocumentableWithChanges(it, wasChanged) }
    }

    private fun <T : Documentable> processMember(member: T): DocumentableWithChanges<T> =
        if (shouldBeSuppressed(member)) DocumentableWithChanges.filteredDocumentable()
        else DocumentableWithChanges(member, false)

    private data class DocumentableWithChanges<T : Documentable>(val documentable: T?, val changed: Boolean = false) {
        companion object {
            fun <T : Documentable> filteredDocumentable(): DocumentableWithChanges<T> =
                DocumentableWithChanges(null, true)
        }
    }
}