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
import org.jetbrains.kotlin.utils.addToStdlib.cast

// TODO: add logging
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
internal object MetadataBuilder {
    fun build(node: CirRootNode, targetIndex: Int): Pair<CommonizerTarget, Collection<KlibModuleMetadata>> =
        node.accept(MetadataBuildingVisitor(), VisitingContext.newContext(node, targetIndex)).cast()

    private class MetadataBuildingVisitor : CirNodeVisitor<VisitingContext, Any?> {
        private val classConsumer = ClassConsumer()

        override fun visitRootNode(node: CirRootNode, context: VisitingContext): Pair<CommonizerTarget, Collection<KlibModuleMetadata>> {
            val modules: Collection<KlibModuleMetadata> = buildMembers(context, node.modules)
            return context.target to modules
        }

        override fun visitModuleNode(node: CirModuleNode, context: VisitingContext): KlibModuleMetadata? {
            val cirModule = context.get<CirModule>(node) ?: return null

            val fragments: MutableCollection<KmModuleFragment> = mutableListOf()
            buildMembers(context, node.packages, destination = fragments)
            addEmptyFragments(fragments)

            return cirModule.buildModule(fragments)
        }

        override fun visitPackageNode(node: CirPackageNode, context: VisitingContext): KmModuleFragment? {
            val cirPackage = context.get<CirPackage>(node) ?: return null
            try {
                buildMembers(context, node.classes, callback = classConsumer::consumeAll)

                val topLevelTypeAliases = mutableListOf<KmTypeAlias>()
                node.typeAliases.values.forEach { typeAliasNode ->
                    when (val classifier = typeAliasNode.accept(this, context)) {
                        null -> Unit
                        is KmClass -> classConsumer.consume(classifier)
                        is KmTypeAlias -> topLevelTypeAliases += classifier
                        else -> error("Unexpected classifier: ${classifier::class.java}, $classifier")
                    }
                }

                linkSealedClassesWithSubclasses(node.fqName, classConsumer)

                val topLevelFunctions: Collection<KmFunction> = buildMembers(context, node.functions)
                val topLevelProperties: Collection<KmProperty> = buildMembers(context, node.properties)

                return cirPackage.buildModuleFragment(classConsumer.allClasses, topLevelTypeAliases, topLevelFunctions, topLevelProperties)
            } finally {
                // Important: clean-up class consumer every time when leaving package
                classConsumer.reset()
            }
        }

        override fun visitPropertyNode(node: CirPropertyNode, context: VisitingContext): KmProperty? {
            return context.get<CirProperty>(node)?.buildProperty(context)
        }

        override fun visitFunctionNode(node: CirFunctionNode, context: VisitingContext): KmFunction? {
            return context.get<CirFunction>(node)?.buildFunction(context)
        }

        override fun visitClassNode(node: CirClassNode, context: VisitingContext): KmClass? {
            val cirClass = context.get<CirClass>(node) ?: return null

            @Suppress("NAME_SHADOWING") val context = if (cirClass.isInner) context else context.topLevelContext
            val classContext = context.childContext(cirClass)

            val directNestedClasses = buildMembers(context = classContext, node.classes, callback = classConsumer::consumeAll)
            val nestedConstructors: Collection<KmConstructor> = buildMembers(context = classContext, node.constructors)
            val nestedFunctions: Collection<KmFunction> = buildMembers(context = classContext, node.functions)
            val nestedProperties: Collection<KmProperty> = buildMembers(context = classContext, node.properties)

            val className = node.classId.asString()

            return cirClass.buildClass(context, className, directNestedClasses, nestedConstructors, nestedFunctions, nestedProperties)
        }

        override fun visitClassConstructorNode(node: CirClassConstructorNode, context: VisitingContext): KmConstructor? {
            return context.get<CirClassConstructor>(node)?.buildClassConstructor(context)
        }

        override fun visitTypeAliasNode(node: CirTypeAliasNode, context: VisitingContext): Any? {
            val cirClassifier = context.get<CirClassifier>(node) ?: return null
            val className = node.classId.asString()

            return when (cirClassifier) {
                is CirTypeAlias -> cirClassifier.buildTypeAlias(context)
                is CirClass -> cirClassifier.buildClass(context, className, emptyList(), emptyList(), emptyList(), emptyList())
                else -> error("Unexpected CIR classifier: ${cirClassifier::class.java}, $cirClassifier")
            }
        }

        private inline fun <reified N : CirNode<*, *>, reified M : Any> buildMembers(
            context: VisitingContext,
            cirMembersNodes: Map<*, N>,
            destination: MutableCollection<M> = mutableListOf(),
            callback: (Collection<M>) -> Unit = {}
        ): Collection<M> {
            return cirMembersNodes.values.mapNotNullTo(destination) { it.accept(this, context) as M? }.also(callback)
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

    fun consumeAll(classes: Collection<KmClass>) = classes.forEach(::consume)

    fun reset() {
        _allClasses.clear()
        _sealedClasses.clear()
    }
}
