/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.mergedtree.buildClassNode
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers

internal class ClassMerger(
    private val classConstructorMerger: ClassConstructorMerger?,
    private val propertyMerger: PropertyMerger?,
    private val functionMerger: FunctionMerger?
) {
    fun processClass(
        context: CirTargetMergingContext,
        ownerNode: CirNodeWithMembers<*, *>,
        classEntry: ClassesToProcess.ClassEntry,
        classesToProcess: ClassesToProcess
    ): Unit = with(context) {
        val classId = classEntry.classId
        val className = classId.relativeNameSegments.last()

        val maybeClassOwnerNode: CirClassNode? = ownerNode as? CirClassNode
        val classNode: CirClassNode = ownerNode.classes.getOrPut(className) {
            buildClassNode(storageManager, targets, classifiers, maybeClassOwnerNode?.commonDeclaration, classId)
        }

        val clazz: KmClass?
        val isEnumEntry: Boolean

        classNode.targetDeclarations[context.targetIndex] = when (classEntry) {
            is ClassesToProcess.ClassEntry.RegularClassEntry -> {
                clazz = classEntry.clazz
                isEnumEntry = Flag.Class.IS_ENUM_ENTRY(clazz.flags)

                CirDeserializers.clazz(className, clazz, context.typeResolver)
            }
            is ClassesToProcess.ClassEntry.EnumEntry -> {
                clazz = null
                isEnumEntry = true

                CirDeserializers.defaultEnumEntry(
                    name = className,
                    annotations = classEntry.annotations,
                    enumClassId = classEntry.enumClassId,
                    enumClass = classEntry.enumClass,
                    typeResolver = context.typeResolver
                )
            }
        }

        if (!isEnumEntry) {
            clazz?.constructors?.forEach { constructor ->
                // TODO: nowhere to read constructor type parameters from
                //val constructorContext = context.create(constructor.typeParameters)
                classConstructorMerger?.processClassConstructor(context, classNode, constructor)
            }
        }

        clazz?.properties?.forEach { property ->
            val propertyContext = context.create(property.typeParameters)
            propertyMerger?.processProperty(propertyContext, classNode, property)
        }
        clazz?.functions?.forEach { function ->
            val functionContext = context.create(function.typeParameters)
            functionMerger?.processFunction(functionContext, classNode, function)
        }

        classesToProcess.forEachClassInScope(parentClassId = classId) { nestedClassEntry ->
            val nestedClassContext = context.create(nestedClassEntry)
            processClass(nestedClassContext, classNode, nestedClassEntry, classesToProcess)
        }
    }
}
