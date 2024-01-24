/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

public abstract class DocumentableReplacerTransformer(
    public val context: DokkaContext
) : PreMergeDocumentableTransformer {
    override fun invoke(modules: List<DModule>): List<DModule> =
        modules.map { module ->
            val (documentable, wasChanged) = processModule(module)
            documentable.takeIf { wasChanged } ?: module
        }

    protected open fun processModule(module: DModule): AnyWithChanges<DModule> {
        val afterProcessing = module.packages.map { processPackage(it) }
        val processedModule = module.takeIf { afterProcessing.none { it.changed } }
            ?: module.copy(packages = afterProcessing.mapNotNull { it.target })
        return AnyWithChanges(processedModule, afterProcessing.any { it.changed })
    }

    protected open fun processPackage(dPackage: DPackage): AnyWithChanges<DPackage> {
        val classlikes = dPackage.classlikes.map { processClassLike(it) }
        val typeAliases = dPackage.typealiases.map { processTypeAlias(it) }
        val functions = dPackage.functions.map { processFunction(it) }
        val properies = dPackage.properties.map { processProperty(it) }

        val wasChanged = (classlikes + typeAliases + functions + properies).any { it.changed }
        return (dPackage.takeIf { !wasChanged } ?: dPackage.copy(
            classlikes = classlikes.mapNotNull { it.target },
            typealiases = typeAliases.mapNotNull { it.target },
            functions = functions.mapNotNull { it.target },
            properties = properies.mapNotNull { it.target }
        )).let { processedPackage -> AnyWithChanges(processedPackage, wasChanged) }
    }

    protected open fun processClassLike(classlike: DClasslike): AnyWithChanges<DClasslike> {
        val functions = classlike.functions.map { processFunction(it) }
        val classlikes = classlike.classlikes.map { processClassLike(it) }
        val properties = classlike.properties.map { processProperty(it) }
        val companion = (classlike as? WithCompanion)?.companion?.let { processClassLike(it) }

        val wasClasslikeChanged = (functions + classlikes + properties).any { it.changed } || companion?.changed == true
        return when (classlike) {
            is DClass -> {
                val constructors = classlike.constructors.map { processFunction(it) }
                val generics = classlike.generics.map { processTypeParameter(it) }
                val wasClassChange =
                    wasClasslikeChanged || constructors.any { it.changed } || generics.any { it.changed }
                (classlike.takeIf { !wasClassChange } ?: classlike.copy(
                    functions = functions.mapNotNull { it.target },
                    classlikes = classlikes.mapNotNull { it.target },
                    properties = properties.mapNotNull { it.target },
                    constructors = constructors.mapNotNull { it.target },
                    generics = generics.mapNotNull { it.target },
                    companion = companion?.target as? DObject
                )).let { AnyWithChanges(it, wasClassChange) }
            }
            is DInterface -> {
                val generics = classlike.generics.map { processTypeParameter(it) }
                val wasInterfaceChange = wasClasslikeChanged || generics.any { it.changed }
                (classlike.takeIf { !wasInterfaceChange } ?: classlike.copy(
                    functions = functions.mapNotNull { it.target },
                    classlikes = classlikes.mapNotNull { it.target },
                    properties = properties.mapNotNull { it.target },
                    generics = generics.mapNotNull { it.target },
                    companion = companion?.target as? DObject
                )).let { AnyWithChanges(it, wasClasslikeChanged) }
            }
            is DObject -> (classlike.takeIf { !wasClasslikeChanged } ?: classlike.copy(
                functions = functions.mapNotNull { it.target },
                classlikes = classlikes.mapNotNull { it.target },
                properties = properties.mapNotNull { it.target },
            )).let { AnyWithChanges(it, wasClasslikeChanged) }
            is DAnnotation -> {
                val constructors = classlike.constructors.map { processFunction(it) }
                val generics = classlike.generics.map { processTypeParameter(it) }
                val wasClassChange =
                    wasClasslikeChanged || constructors.any { it.changed } || generics.any { it.changed }
                (classlike.takeIf { !wasClassChange } ?: classlike.copy(
                    functions = functions.mapNotNull { it.target },
                    classlikes = classlikes.mapNotNull { it.target },
                    properties = properties.mapNotNull { it.target },
                    constructors = constructors.mapNotNull { it.target },
                    generics = generics.mapNotNull { it.target },
                    companion = companion?.target as? DObject
                )).let { AnyWithChanges(it, wasClassChange) }
            }
            is DEnum -> {
                val constructors = classlike.constructors.map { processFunction(it) }
                val entries = classlike.entries.map { processEnumEntry(it) }
                val wasClassChange =
                    wasClasslikeChanged || (constructors + entries).any { it.changed }
                (classlike.takeIf { !wasClassChange } ?: classlike.copy(
                    functions = functions.mapNotNull { it.target },
                    classlikes = classlikes.mapNotNull { it.target },
                    properties = properties.mapNotNull { it.target },
                    constructors = constructors.mapNotNull { it.target },
                    companion = companion?.target as? DObject,
                    entries = entries.mapNotNull { it.target }
                )).let { AnyWithChanges(it, wasClassChange) }
            }
        }
    }

    protected open fun processEnumEntry(dEnumEntry: DEnumEntry): AnyWithChanges<DEnumEntry> {
        val functions = dEnumEntry.functions.map { processFunction(it) }
        val properties = dEnumEntry.properties.map { processProperty(it) }
        val classlikes = dEnumEntry.classlikes.map { processClassLike(it) }

        val wasChanged = (functions + properties + classlikes).any { it.changed }
        return (dEnumEntry.takeIf { !wasChanged } ?: dEnumEntry.copy(
            functions = functions.mapNotNull { it.target },
            classlikes = classlikes.mapNotNull { it.target },
            properties = properties.mapNotNull { it.target },
        )).let { AnyWithChanges(it, wasChanged) }
    }

    protected open fun processFunction(dFunction: DFunction): AnyWithChanges<DFunction> {
        val type = processBound(dFunction.type)
        val parameters = dFunction.parameters.map { processParameter(it) }
        val receiver = dFunction.receiver?.let { processParameter(it) }
        val generics = dFunction.generics.map { processTypeParameter(it) }

        val wasChanged = parameters.any { it.changed } || receiver?.changed == true
                || type.changed || generics.any { it.changed }
        return (dFunction.takeIf { !wasChanged } ?: dFunction.copy(
            type = type.target ?: dFunction.type,
            parameters = parameters.mapNotNull { it.target },
            receiver = receiver?.target,
            generics = generics.mapNotNull { it.target },
        )).let { AnyWithChanges(it, wasChanged) }
    }

    protected open fun processProperty(dProperty: DProperty): AnyWithChanges<DProperty> {
        val getter = dProperty.getter?.let { processFunction(it) }
        val setter = dProperty.setter?.let { processFunction(it) }
        val type = processBound(dProperty.type)
        val generics = dProperty.generics.map { processTypeParameter(it) }

        val wasChanged = getter?.changed == true || setter?.changed == true
                || type.changed || generics.any { it.changed }
        return (dProperty.takeIf { !wasChanged } ?: dProperty.copy(
            type = type.target ?: dProperty.type,
            setter = setter?.target,
            getter = getter?.target,
            generics = generics.mapNotNull { it.target }
        )).let { AnyWithChanges(it, wasChanged) }
    }

    protected open fun processParameter(dParameter: DParameter): AnyWithChanges<DParameter> {
        val type = processBound(dParameter.type)

        val wasChanged = type.changed
        return (dParameter.takeIf { !wasChanged } ?: dParameter.copy(
            type = type.target ?: dParameter.type,
        )).let { AnyWithChanges(it, wasChanged) }
    }

    protected open fun processTypeParameter(dTypeParameter: DTypeParameter): AnyWithChanges<DTypeParameter> {
        val bounds = dTypeParameter.bounds.map { processBound(it) }

        val wasChanged = bounds.any { it.changed }
        return (dTypeParameter.takeIf { !wasChanged } ?: dTypeParameter.copy(
            bounds = bounds.mapIndexed { i, v -> v.target ?: dTypeParameter.bounds[i] }
        )).let { AnyWithChanges(it, wasChanged) }
    }

    protected open fun processBound(bound: Bound): AnyWithChanges<Bound> {
        return when(bound) {
            is GenericTypeConstructor -> processGenericTypeConstructor(bound)
            is FunctionalTypeConstructor -> processFunctionalTypeConstructor(bound)
            else -> AnyWithChanges(bound, false)
        }
    }

    protected open fun processVariance(variance: Variance<*>): AnyWithChanges<Variance<*>> {
        val bound = processBound(variance.inner)
        if (!bound.changed)
            return AnyWithChanges(variance, false)
        return when (variance) {
            is Covariance<*> -> AnyWithChanges(
                Covariance(bound.target ?: variance.inner), true)
            is Contravariance<*> -> AnyWithChanges(
                Contravariance(bound.target ?: variance.inner), true)
            is Invariance<*> -> AnyWithChanges(
                Invariance(bound.target ?: variance.inner), true)
            else -> AnyWithChanges(variance, false)
        }
    }

    protected open fun processProjection(projection: Projection): AnyWithChanges<Projection> =
        when (projection) {
            is Bound -> processBound(projection)
            is Variance<Bound> -> processVariance(projection)
            else -> AnyWithChanges(projection, false)
        }

    protected open fun processGenericTypeConstructor(
        genericTypeConstructor: GenericTypeConstructor
    ): AnyWithChanges<GenericTypeConstructor> {
        val projections = genericTypeConstructor.projections.map { processProjection(it) }

        val wasChanged = projections.any { it.changed }
        return (genericTypeConstructor.takeIf { !wasChanged } ?: genericTypeConstructor.copy(
            projections = projections.mapNotNull { it.target }
        )).let { AnyWithChanges(it, wasChanged) }
    }

    protected open fun processFunctionalTypeConstructor(
        functionalTypeConstructor: FunctionalTypeConstructor
    ): AnyWithChanges<FunctionalTypeConstructor> {
        val projections = functionalTypeConstructor.projections.map { processProjection(it) }

        val wasChanged = projections.any { it.changed }
        return (functionalTypeConstructor.takeIf { !wasChanged } ?: functionalTypeConstructor.copy(
            projections = projections.mapNotNull { it.target }
        )).let { AnyWithChanges(it, wasChanged) }
    }

    protected open fun processTypeAlias(dTypeAlias: DTypeAlias): AnyWithChanges<DTypeAlias> {
        val underlyingType = dTypeAlias.underlyingType.mapValues { processBound(it.value) }
        val generics = dTypeAlias.generics.map { processTypeParameter(it) }

        val wasChanged = underlyingType.any { it.value.changed } || generics.any { it.changed }
        return (dTypeAlias.takeIf { !wasChanged } ?: dTypeAlias.copy(
            underlyingType = underlyingType.mapValues { it.value.target ?: dTypeAlias.underlyingType.getValue(it.key) },
            generics = generics.mapNotNull { it.target }
        )).let { AnyWithChanges(it, wasChanged) }
    }


    protected data class AnyWithChanges<out T>(val target: T?, val changed: Boolean = false)
}
