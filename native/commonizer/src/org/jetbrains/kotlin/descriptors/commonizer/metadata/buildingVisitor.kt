/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.stats.DeclarationType
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector
import org.jetbrains.kotlin.descriptors.commonizer.stats.StatsCollector.StatsKey
import org.jetbrains.kotlin.descriptors.commonizer.utils.DEFAULT_CONSTRUCTOR_NAME
import org.jetbrains.kotlin.descriptors.commonizer.utils.strip
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.descriptors.commonizer.utils.firstNonNull
import org.jetbrains.kotlin.descriptors.commonizer.metadata.MetadataBuildingVisitorContext.Path

internal object MetadataBuilder {
    fun build(
        node: CirRootNode,
        targetIndex: Int,
        statsCollector: StatsCollector?
    ): Pair<CommonizerTarget, Collection<KlibModuleMetadata>> {
        return node.accept(
            MetadataBuildingVisitor(statsCollector),
            MetadataBuildingVisitorContext.rootContext(node, targetIndex)
        ).cast()
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
private class MetadataBuildingVisitor(private val statsCollector: StatsCollector?) : CirNodeVisitor<MetadataBuildingVisitorContext, Any?> {
    private val classConsumer = ClassConsumer()

    override fun visitRootNode(
        node: CirRootNode,
        rootContext: MetadataBuildingVisitorContext
    ): Pair<CommonizerTarget, Collection<KlibModuleMetadata>> {
        val modules: List<KlibModuleMetadata> = node.modules.mapNotNull { (moduleName, moduleNode) ->
            val moduleContext = rootContext.moduleContext(moduleName)
            val module: KlibModuleMetadata? = moduleNode.accept(this, moduleContext)?.cast()
            statsCollector?.logModule(moduleContext)
            module
        }

        return rootContext.target to modules
    }

    override fun visitModuleNode(
        node: CirModuleNode,
        moduleContext: MetadataBuildingVisitorContext
    ): KlibModuleMetadata? {
        val cirModule = moduleContext.get<CirModule>(node) ?: return null

        val fragments: MutableCollection<KmModuleFragment> = mutableListOf()
        node.packages.mapNotNullTo(fragments) { (packageFqName, packageNode) ->
            val packageContext = moduleContext.packageContext(packageFqName)
            packageNode.accept(this, packageContext)?.cast()
        }

        addEmptyFragments(fragments)

        return cirModule.buildModule(fragments)
    }

    override fun visitPackageNode(
        node: CirPackageNode,
        packageContext: MetadataBuildingVisitorContext
    ): KmModuleFragment? {
        val cirPackage = packageContext.get<CirPackage>(node) ?: return null

        try {
            node.classes.forEach { (className, classNode) ->
                val classContext = packageContext.classifierContext(className)
                val clazz: KmClass = classNode.accept(this, classContext)?.cast() ?: return@forEach
                classConsumer.consume(clazz)
                statsCollector?.logClass(clazz, classContext)
            }

            val topLevelTypeAliases = mutableListOf<KmTypeAlias>()
            node.typeAliases.forEach { (typeAliasName, typeAliasNode) ->
                val typeAliasContext = packageContext.classifierContext(typeAliasName)
                when (val classifier = typeAliasNode.accept(this, typeAliasContext)) {
                    null -> Unit
                    is KmClass -> {
                        classConsumer.consume(classifier)
                        statsCollector?.logClass(classifier, typeAliasContext)
                    }
                    is KmTypeAlias -> {
                        topLevelTypeAliases += classifier
                        statsCollector?.logTypeAlias(typeAliasContext)
                    }
                    else -> error("Unexpected classifier: ${classifier::class.java}, $classifier")
                }
            }

            linkSealedClassesWithSubclasses(cirPackage.fqName, classConsumer)

            val topLevelFunctions: Collection<KmFunction> = node.functions.mapNotNull { (functionKey, functionNode) ->
                val functionContext = packageContext.callableMemberContext(functionKey.name)
                val function: KmFunction = functionNode.accept(this, functionContext)?.cast() ?: return@mapNotNull null
                statsCollector?.logFunction(function, functionContext, functionKey)
                function
            }

            val topLevelProperties: Collection<KmProperty> = node.properties.mapNotNull { (propertyKey, propertyNode) ->
                val propertyContext = packageContext.callableMemberContext(propertyKey.name)
                val property: KmProperty = propertyNode.accept(this, propertyContext)?.cast() ?: return@mapNotNull null
                statsCollector?.logProperty(propertyContext, propertyKey, propertyNode)
                property
            }

            return cirPackage.buildModuleFragment(classConsumer.allClasses, topLevelTypeAliases, topLevelFunctions, topLevelProperties)
        } finally {
            // Important: clean-up class consumer every time when leaving package
            classConsumer.reset()
        }
    }

    override fun visitPropertyNode(
        node: CirPropertyNode,
        propertyContext: MetadataBuildingVisitorContext
    ): KmProperty? {
        return propertyContext.get<CirProperty>(node)?.buildProperty(propertyContext)
    }

    override fun visitFunctionNode(
        node: CirFunctionNode,
        functionContext: MetadataBuildingVisitorContext
    ): KmFunction? {
        return functionContext.get<CirFunction>(node)?.buildFunction(functionContext)
    }

    override fun visitClassNode(
        node: CirClassNode,
        classContext: MetadataBuildingVisitorContext
    ): KmClass? {
        val cirClass = classContext.get<CirClass>(node) ?: return null
        val classTypeParametersCount = cirClass.typeParameters.size
        val fullClassName = classContext.currentPath.toString()

        val directNestedClasses: Collection<KmClass> = node.classes.mapNotNull { (nestedClassName, nestedClassNode) ->
            val nestedClassContext = classContext.classifierContext(nestedClassName, classTypeParametersCount)
            val nestedClass: KmClass = nestedClassNode.accept(this, nestedClassContext)?.cast() ?: return@mapNotNull null
            classConsumer.consume(nestedClass)
            statsCollector?.logClass(nestedClass, nestedClassContext)
            nestedClass
        }

        val nestedConstructors: Collection<KmConstructor> = node.constructors.mapNotNull { (constructorKey, constructorNode) ->
            val constructorContext = classContext.callableMemberContext(DEFAULT_CONSTRUCTOR_NAME, classTypeParametersCount)
            val constructor: KmConstructor = constructorNode.accept(this, constructorContext)?.cast() ?: return@mapNotNull null
            statsCollector?.logClassConstructor(constructor, constructorContext, constructorKey)
            constructor
        }

        val nestedFunctions: Collection<KmFunction> = node.functions.mapNotNull { (functionKey, functionNode) ->
            val functionContext = classContext.callableMemberContext(functionKey.name, classTypeParametersCount)
            val function: KmFunction = functionNode.accept(this, functionContext)?.cast() ?: return@mapNotNull null
            statsCollector?.logFunction(function, functionContext, functionKey)
            function
        }

        val nestedProperties: Collection<KmProperty> = node.properties.mapNotNull { (propertyKey, propertyNode) ->
            val propertyContext = classContext.callableMemberContext(propertyKey.name, classTypeParametersCount)
            val property: KmProperty = propertyNode.accept(this, propertyContext)?.cast() ?: return@mapNotNull null
            statsCollector?.logProperty(propertyContext, propertyKey, propertyNode)
            property
        }

        return cirClass.buildClass(classContext, fullClassName, directNestedClasses, nestedConstructors, nestedFunctions, nestedProperties)
    }

    override fun visitClassConstructorNode(
        node: CirClassConstructorNode,
        constructorContext: MetadataBuildingVisitorContext
    ): KmConstructor? {
        return constructorContext.get<CirClassConstructor>(node)?.buildClassConstructor(constructorContext)
    }

    override fun visitTypeAliasNode(
        node: CirTypeAliasNode,
        typeAliasContext: MetadataBuildingVisitorContext
    ): Any? {
        val cirClassifier = typeAliasContext.get<CirClassifier>(node) ?: return null

        return when (cirClassifier) {
            is CirTypeAlias -> cirClassifier.buildTypeAlias(typeAliasContext)
            is CirClass -> {
                val fullClassName = typeAliasContext.currentPath.toString()
                cirClassifier.buildClass(typeAliasContext, fullClassName, emptyList(), emptyList(), emptyList(), emptyList())
            }
            else -> error("Unexpected CIR classifier: ${cirClassifier::class.java}, $cirClassifier")
        }
    }

    companion object {
        private fun StatsCollector.logModule(
            moduleContext: MetadataBuildingVisitorContext
        ) = logDeclaration(moduleContext.targetIndex) {
            StatsKey(moduleContext.currentPath.toString(), DeclarationType.MODULE)
        }

        private fun StatsCollector.logClass(
            clazz: KmClass,
            classContext: MetadataBuildingVisitorContext
        ) = logDeclaration(classContext.targetIndex) {
            val declarationType = when {
                Flag.Class.IS_ENUM_CLASS(clazz.flags) -> DeclarationType.ENUM_CLASS
                Flag.Class.IS_ENUM_ENTRY(clazz.flags) -> DeclarationType.ENUM_ENTRY
                Flag.Class.IS_INTERFACE(clazz.flags) -> when {
                    (classContext.currentPath as Path.Classifier).classifierId.isNestedClass -> DeclarationType.NESTED_INTERFACE
                    else -> DeclarationType.TOP_LEVEL_INTERFACE
                }
                else -> when {
                    Flag.Class.IS_COMPANION_OBJECT(clazz.flags) -> DeclarationType.COMPANION_OBJECT
                    (classContext.currentPath as Path.Classifier).classifierId.isNestedClass -> DeclarationType.NESTED_CLASS
                    else -> DeclarationType.TOP_LEVEL_CLASS
                }
            }

            StatsKey(classContext.currentPath.toString(), declarationType)
        }

        private fun StatsCollector.logTypeAlias(
            typeAliasContext: MetadataBuildingVisitorContext
        ) = logDeclaration(typeAliasContext.targetIndex) {
            StatsKey(typeAliasContext.currentPath.toString(), DeclarationType.TYPE_ALIAS)
        }

        private fun StatsCollector.logProperty(
            propertyContext: MetadataBuildingVisitorContext,
            propertyKey: PropertyApproximationKey,
            propertyNode: CirPropertyNode
        ) = logDeclaration(propertyContext.targetIndex) {
            val declarationType = when {
                (propertyContext.currentPath as Path.CallableMember).memberId.isNestedClass -> DeclarationType.NESTED_VAL
                propertyNode.targetDeclarations.firstNonNull().isConst -> DeclarationType.TOP_LEVEL_CONST_VAL
                else -> DeclarationType.TOP_LEVEL_VAL
            }

            StatsKey(
                id = propertyContext.currentPath.toString(),
                extensionReceiver = propertyKey.extensionReceiverParameterType,
                parameterNames = emptyList(),
                parameterTypes = emptyList(),
                declarationType = declarationType
            )
        }

        private fun StatsCollector.logFunction(
            function: KmFunction,
            functionContext: MetadataBuildingVisitorContext,
            functionKey: FunctionApproximationKey
        ) = logDeclaration(functionContext.targetIndex) {
            val declarationType = when {
                (functionContext.currentPath as Path.CallableMember).memberId.isNestedClass -> DeclarationType.NESTED_FUN
                else -> DeclarationType.TOP_LEVEL_FUN
            }

            StatsKey(
                id = functionContext.currentPath.toString(),
                extensionReceiver = functionKey.extensionReceiverParameterType,
                parameterNames = function.valueParameters.map { it.name },
                parameterTypes = functionKey.valueParametersTypes.asList(),
                declarationType = declarationType
            )
        }

        private fun StatsCollector.logClassConstructor(
            constructor: KmConstructor,
            constructorContext: MetadataBuildingVisitorContext,
            constructorKey: ConstructorApproximationKey
        ) = logDeclaration(constructorContext.targetIndex) {
            StatsKey(
                id = constructorContext.currentPath.toString(),
                extensionReceiver = null,
                parameterNames = constructor.valueParameters.map { it.name },
                parameterTypes = constructorKey.valueParametersTypes.asList(),
                declarationType = DeclarationType.CLASS_CONSTRUCTOR
            )
        }
    }
}

internal data class MetadataBuildingVisitorContext(
    val targetIndex: Int,
    val target: CommonizerTarget,
    val isCommon: Boolean,
    val typeParameterIndexOffset: Int,
    val currentPath: Path
) {
    sealed class Path {
        object Empty : Path() {
            override fun toString() = ""
        }

        @Suppress("MemberVisibilityCanBePrivate")
        class Module(val moduleName: Name) : Path() {
            override fun toString() = moduleName.strip()
        }

        class Package(val packageFqName: FqName) : Path() {
            fun nestedClassifier(classifierName: Name) = Classifier(ClassId(packageFqName, classifierName))
            fun nestedCallableMember(memberName: Name) = CallableMember(ClassId(packageFqName, memberName))

            override fun toString() = packageFqName.asString()
        }

        class Classifier(val classifierId: ClassId) : Path() {
            fun nestedClassifier(classifierName: Name) = Classifier(classifierId.createNestedClassId(classifierName))
            fun nestedCallableMember(memberName: Name) = CallableMember(classifierId.createNestedClassId(memberName))

            override fun toString() = classifierId.asString()
        }

        class CallableMember(val memberId: ClassId) : Path() {
            override fun toString() = memberId.asString()
        }
    }

    fun moduleContext(moduleName: Name): MetadataBuildingVisitorContext {
        check(moduleName.isSpecial)
        check(currentPath is Path.Empty)

        return MetadataBuildingVisitorContext(
            targetIndex = targetIndex,
            target = target,
            isCommon = isCommon,
            typeParameterIndexOffset = 0,
            currentPath = Path.Module(moduleName)
        )
    }

    fun packageContext(packageFqName: FqName): MetadataBuildingVisitorContext {
        check(currentPath is Path.Module)

        return MetadataBuildingVisitorContext(
            targetIndex = targetIndex,
            target = target,
            isCommon = isCommon,
            typeParameterIndexOffset = 0,
            currentPath = Path.Package(packageFqName)
        )
    }

    fun classifierContext(
        classifierName: Name,
        outerClassTypeParametersCount: Int = 0
    ): MetadataBuildingVisitorContext {
        val newPath = when (currentPath) {
            is Path.Package -> {
                check(outerClassTypeParametersCount == 0)
                currentPath.nestedClassifier(classifierName)
            }
            is Path.Classifier -> {
                check(outerClassTypeParametersCount >= 0)
                currentPath.nestedClassifier(classifierName)
            }
            else -> error("Illegal state")
        }

        return MetadataBuildingVisitorContext(
            targetIndex = targetIndex,
            target = target,
            isCommon = isCommon,
            typeParameterIndexOffset = typeParameterIndexOffset + outerClassTypeParametersCount,
            currentPath = newPath
        )
    }

    fun callableMemberContext(
        memberName: Name,
        ownerClassTypeParametersCount: Int = 0
    ): MetadataBuildingVisitorContext {
        val newPath = when (currentPath) {
            is Path.Package -> {
                check(ownerClassTypeParametersCount == 0)
                currentPath.nestedCallableMember(memberName)
            }
            is Path.Classifier -> {
                check(ownerClassTypeParametersCount >= 0)
                currentPath.nestedCallableMember(memberName)
            }
            else -> error("Illegal state")
        }

        return MetadataBuildingVisitorContext(
            targetIndex = targetIndex,
            target = target,
            isCommon = isCommon,
            typeParameterIndexOffset = typeParameterIndexOffset + ownerClassTypeParametersCount,
            currentPath = newPath
        )
    }

    inline fun <reified T : CirDeclaration> get(node: CirNode<*, *>): T? {
        return (if (isCommon) node.commonDeclaration() else node.targetDeclarations[targetIndex]) as T?
    }

    inline fun <reified T : CirDeclaration> get(node: CirNodeWithLiftingUp<*, *>): T? {
        return when {
            isCommon -> node.commonDeclaration() as T?
            node.isLiftedUp -> null
            else -> node.targetDeclarations[targetIndex] as T?
        }
    }

    companion object {
        fun rootContext(rootNode: CirRootNode, targetIndex: Int): MetadataBuildingVisitorContext {
            val isCommon = rootNode.indexOfCommon == targetIndex
            val target = (if (isCommon) rootNode.commonDeclaration() else rootNode.targetDeclarations[targetIndex])!!.target

            return MetadataBuildingVisitorContext(
                targetIndex = targetIndex,
                target = target,
                isCommon = isCommon,
                typeParameterIndexOffset = 0,
                currentPath = Path.Empty
            )
        }
    }
}

internal class ClassConsumer {
    private val _allClasses = mutableListOf<KmClass>()
    private val _sealedClasses = mutableListOf<KmClass>()

    val allClasses: Collection<KmClass> get() = _allClasses
    val sealedClasses: Collection<KmClass> get() = _sealedClasses

    fun consume(clazz: KmClass) {
        _allClasses += clazz
        if (Flag.Common.IS_SEALED(clazz.flags)) _sealedClasses += clazz
    }

    fun reset() {
        _allClasses.clear()
        _sealedClasses.clear()
    }
}
