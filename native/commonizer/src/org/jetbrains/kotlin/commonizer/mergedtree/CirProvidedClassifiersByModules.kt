/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import com.intellij.util.containers.FactoryMap
import org.jetbrains.kotlin.commonizer.ModulesProvider
import org.jetbrains.kotlin.commonizer.ModulesProvider.CInteropModuleAttributes
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.commonizer.cir.CirProvided
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers.Companion.FALLBACK_FORWARD_DECLARATION_CLASS
import org.jetbrains.kotlin.commonizer.utils.*
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.types.Variance

internal class CirProvidedClassifiersByModules internal constructor(
    private val hasForwardDeclarations: Boolean,
    private val classifiers: Map<CirEntityId, CirProvided.Classifier>,
) : CirProvidedClassifiers {

    private val typeAliasesByUnderlyingTypes = run {
        CommonizerMap<CirEntityId, MutableList<CirEntityId>>().also { map ->
            classifiers.forEach { (id, classifier) ->
                if (classifier is CirProvided.TypeAlias) {
                    val set = map.computeIfAbsent(classifier.underlyingType.classifierId) { ArrayList() }
                    set.add(id)
                }
            }
        }
    }

    override fun hasClassifier(classifierId: CirEntityId) =
        if (classifierId.packageName.isUnderKotlinNativeSyntheticPackages) {
            hasForwardDeclarations
        } else {
            classifierId in classifiers
        }

    override fun findTypeAliasesWithUnderlyingType(underlyingClassifier: CirEntityId): List<CirEntityId> {
        return typeAliasesByUnderlyingTypes[underlyingClassifier].orEmpty()
    }

    override fun classifier(classifierId: CirEntityId) =
        classifiers[classifierId] ?: if (hasForwardDeclarations && classifierId.packageName.isUnderKotlinNativeSyntheticPackages)
            FALLBACK_FORWARD_DECLARATION_CLASS else null

    companion object {
        fun load(modulesProvider: ModulesProvider): CirProvidedClassifiers {
            val classifiers = CommonizerMap<CirEntityId, CirProvided.Classifier>()

            modulesProvider.moduleInfos.forEach { moduleInfo ->
                val metadata = modulesProvider.loadModuleMetadata(moduleInfo.name)
                readModule(metadata, classifiers::set)
            }

            if (classifiers.isEmpty())
                return CirProvidedClassifiers.EMPTY

            return CirProvidedClassifiersByModules(false, classifiers)
        }

        /**
         * Will load *all* forward declarations provided by all modules into a flat [CirProvidedClassifiers].
         * Note: This builds a union *not an intersection* of forward declarations.
         */
        fun loadExportedForwardDeclarations(modulesProviders: List<ModulesProvider>): CirProvidedClassifiers {
            val classifiers = CommonizerMap<CirEntityId, CirProvided.Classifier>()

            modulesProviders.flatMap { moduleProvider -> moduleProvider.moduleInfos }
                .mapNotNull { moduleInfo -> moduleInfo.cInteropAttributes }
                .forEach { attrs -> readExportedForwardDeclarations(attrs, classifiers::set) }

            if (classifiers.isEmpty()) return CirProvidedClassifiers.EMPTY
            return CirProvidedClassifiersByModules(true, classifiers)
        }

    }
}

private fun readExportedForwardDeclarations(
    cInteropAttributes: CInteropModuleAttributes,
    consumer: (CirEntityId, CirProvided.Classifier) -> Unit
) {
    val exportedForwardDeclarations = cInteropAttributes.exportedForwardDeclarations
    if (exportedForwardDeclarations.isEmpty()) return

    val mainPackageName = CirPackageName.create(cInteropAttributes.mainPackage)

    exportedForwardDeclarations.forEach { classFqName ->
        // Class has synthetic package FQ name (cnames/objcnames). Need to transfer it to the main package.
        val syntheticPackageName = CirPackageName.create(classFqName.substringBeforeLast('.', missingDelimiterValue = ""))
        val className = CirName.create(classFqName.substringAfterLast('.'))

        val syntheticClassId = CirEntityId.create(syntheticPackageName, className)
        val aliasedClassId = CirEntityId.create(mainPackageName, className)

        val clazz = CirProvided.ExportedForwardDeclarationClass(syntheticClassId)

        consumer(syntheticClassId, clazz)
        consumer(aliasedClassId, clazz)
    }
}

private fun readModule(metadata: SerializedMetadata, consumer: (CirEntityId, CirProvided.Classifier) -> Unit) {
    for (i in metadata.fragmentNames.indices) {
        val packageFqName = metadata.fragmentNames[i]
        val packageFragments = metadata.fragments[i]

        val classProtosToRead = ClassProtosToRead()

        for (j in packageFragments.indices) {
            val packageFragmentProto = parsePackageFragment(packageFragments[j])

            val classProtos: List<ProtoBuf.Class> = packageFragmentProto.class_List
            val typeAliasProtos: List<ProtoBuf.TypeAlias> = packageFragmentProto.`package`?.typeAliasList.orEmpty()

            if (classProtos.isEmpty() && typeAliasProtos.isEmpty())
                continue

            val packageName = CirPackageName.create(packageFqName)
            val strings = NameResolverImpl(packageFragmentProto.strings, packageFragmentProto.qualifiedNames)

            classProtosToRead.addClasses(classProtos, strings)

            if (typeAliasProtos.isNotEmpty()) {
                val types = TypeTable(packageFragmentProto.`package`.typeTable)
                for (typeAliasProto in typeAliasProtos) {
                    readTypeAlias(typeAliasProto, packageName, strings, types, consumer)
                }
            }
        }

        classProtosToRead.forEachClassInScope(parentClassId = null) { classEntry ->
            readClass(classEntry, classProtosToRead, typeParameterIndexOffset = 0, consumer)
        }
    }
}

private class ClassProtosToRead {
    data class ClassEntry(
        val classId: CirEntityId, val proto: ProtoBuf.Class, val strings: NameResolver
    )

    // key = parent class ID (or NON_EXISTING_CLASSIFIER_ID for top-level classes)
    // value = class protos under this parent class (MutableList to preserve order of classes)
    private val groupedByParentClassId = FactoryMap.create<CirEntityId, MutableList<ClassEntry>> { ArrayList() }

    fun addClasses(classProtos: List<ProtoBuf.Class>, strings: NameResolver) {
        classProtos.forEach { classProto ->
            if (strings.isLocalClassName(classProto.fqName)) return@forEach

            val classId = CirEntityId.create(strings.getQualifiedClassName(classProto.fqName))
            val parentClassId: CirEntityId = classId.getParentEntityId() ?: NON_EXISTING_CLASSIFIER_ID

            groupedByParentClassId.getValue(parentClassId) += ClassEntry(classId, classProto, strings)
        }
    }

    fun forEachClassInScope(parentClassId: CirEntityId?, block: (ClassEntry) -> Unit) {
        groupedByParentClassId[parentClassId ?: NON_EXISTING_CLASSIFIER_ID]?.forEach { classEntry -> block(classEntry) }
    }
}

private fun readClass(
    classEntry: ClassProtosToRead.ClassEntry,
    classProtosToRead: ClassProtosToRead,
    typeParameterIndexOffset: Int,
    consumer: (CirEntityId, CirProvided.Classifier) -> Unit
) {
    val (classId, classProto) = classEntry

    val typeParameterNameToIndex = HashMap<Int, Int>()

    val typeParameters = readTypeParameters(
        typeParameterProtos = classProto.typeParameterList,
        typeParameterIndexOffset = typeParameterIndexOffset,
        nameToIndexMapper = typeParameterNameToIndex::set
    )
    val typeReadContext = TypeReadContext(classEntry.strings, TypeTable(classProto.typeTable), typeParameterNameToIndex)

    val supertypes = (classProto.supertypeList.map { readType(it, typeReadContext) } +
            classProto.supertypeIdList.map { readType(classProto.typeTable.getType(it), typeReadContext) })
        .filterNot { type -> type is CirProvided.ClassType && type.classifierId == ANY_CLASS_ID }


    val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(classProto.flags))
    val kind = ProtoEnumFlags.classKind(Flags.CLASS_KIND.get(classProto.flags))
    val clazz = CirProvided.RegularClass(typeParameters, supertypes, visibility, kind)

    consumer(classId, clazz)

    classProtosToRead.forEachClassInScope(parentClassId = classId) { nestedClassEntry ->
        readClass(nestedClassEntry, classProtosToRead, typeParameterIndexOffset = typeParameters.size + typeParameterIndexOffset, consumer)
    }
}

private inline fun readTypeAlias(
    typeAliasProto: ProtoBuf.TypeAlias,
    packageName: CirPackageName,
    strings: NameResolver,
    types: TypeTable,
    consumer: (CirEntityId, CirProvided.Classifier) -> Unit
) {
    val typeAliasId = CirEntityId.create(packageName, CirName.create(strings.getString(typeAliasProto.name)))

    val typeParameterNameToIndex = HashMap<Int, Int>()
    val typeParameters = readTypeParameters(
        typeParameterProtos = typeAliasProto.typeParameterList,
        typeParameterIndexOffset = 0,
        nameToIndexMapper = typeParameterNameToIndex::set
    )

    val underlyingType = readType(typeAliasProto.underlyingType(types), TypeReadContext(strings, types, typeParameterNameToIndex))
    val typeAlias = CirProvided.TypeAlias(typeParameters, underlyingType as CirProvided.ClassOrTypeAliasType)

    consumer(typeAliasId, typeAlias)
}

private inline fun readTypeParameters(
    typeParameterProtos: List<ProtoBuf.TypeParameter>,
    typeParameterIndexOffset: Int,
    nameToIndexMapper: (name: Int, id: Int) -> Unit = { _, _ -> }
): List<CirProvided.TypeParameter> =
    typeParameterProtos.compactMapIndexed { localIndex, typeParameterProto ->
        val index = localIndex + typeParameterIndexOffset
        val typeParameter = CirProvided.TypeParameter(
            index = index,
            variance = readVariance(typeParameterProto.variance)
        )
        nameToIndexMapper(typeParameterProto.name, index)
        typeParameter
    }

private class TypeReadContext(
    val strings: NameResolver,
    val types: TypeTable,
    private val _typeParameterNameToIndex: Map<Int, Int>
) {
    val typeParameterNameToIndex: (Int) -> Int = { name ->
        _typeParameterNameToIndex[name] ?: error("No type parameter index for ${strings.getString(name)}")
    }

    private val _typeParameterIdToIndex = HashMap<Int, Int>()
    val typeParameterIdToIndex: (Int) -> Int = { id -> _typeParameterIdToIndex.getOrPut(id) { _typeParameterIdToIndex.size } }
}

private fun readType(typeProto: ProtoBuf.Type, context: TypeReadContext): CirProvided.Type =
    with(typeProto.abbreviatedType(context.types) ?: typeProto) {
        when {
            hasClassName() -> {
                val classId = CirEntityId.create(context.strings.getQualifiedClassName(className))
                val outerType = typeProto.outerType(context.types)?.let { outerType ->
                    val outerClassType = readType(outerType, context)
                    check(outerClassType is CirProvided.ClassType) { "Outer type of $classId is not a class: $outerClassType" }
                    outerClassType
                }

                CirProvided.ClassType(
                    classifierId = classId,
                    outerType = outerType,
                    arguments = readTypeArguments(argumentList, context),
                    isMarkedNullable = nullable
                )
            }
            hasTypeAliasName() -> CirProvided.TypeAliasType(
                classifierId = CirEntityId.create(context.strings.getQualifiedClassName(typeAliasName)),
                arguments = readTypeArguments(argumentList, context),
                isMarkedNullable = nullable
            )
            hasTypeParameter() -> CirProvided.TypeParameterType(
                index = context.typeParameterIdToIndex(typeParameter),
                isMarkedNullable = nullable
            )
            hasTypeParameterName() -> CirProvided.TypeParameterType(
                index = context.typeParameterNameToIndex(typeParameterName),
                isMarkedNullable = nullable
            )
            else -> error("No classifier (class, type alias or type parameter) recorded for Type")
        }
    }

private fun readTypeArguments(argumentProtos: List<ProtoBuf.Type.Argument>, context: TypeReadContext): List<CirProvided.TypeProjection> =
    argumentProtos.compactMap { argumentProto ->
        val variance = readVariance(argumentProto.projection!!) ?: return@compactMap CirProvided.StarTypeProjection
        val typeProto = argumentProto.type(context.types) ?: error("No type argument for non-STAR projection in Type")

        CirProvided.RegularTypeProjection(
            variance = variance,
            type = readType(typeProto, context)
        )
    }

@Suppress("NOTHING_TO_INLINE")
private inline fun readVariance(varianceProto: ProtoBuf.TypeParameter.Variance): Variance =
    when (varianceProto) {
        ProtoBuf.TypeParameter.Variance.IN -> Variance.IN_VARIANCE
        ProtoBuf.TypeParameter.Variance.OUT -> Variance.OUT_VARIANCE
        ProtoBuf.TypeParameter.Variance.INV -> Variance.INVARIANT
    }

@Suppress("NOTHING_TO_INLINE")
private inline fun readVariance(varianceProto: ProtoBuf.Type.Argument.Projection): Variance? =
    when (varianceProto) {
        ProtoBuf.Type.Argument.Projection.IN -> Variance.IN_VARIANCE
        ProtoBuf.Type.Argument.Projection.OUT -> Variance.OUT_VARIANCE
        ProtoBuf.Type.Argument.Projection.INV -> Variance.INVARIANT
        ProtoBuf.Type.Argument.Projection.STAR -> null
    }
