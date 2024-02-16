/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import kotlin.metadata.*
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.tree.CirTreeClass

internal class CirTreeClassDeserializer(
    private val propertyDeserializer: CirTreePropertyDeserializer,
    private val functionDeserializer: CirTreeFunctionDeserializer,
    private val constructorDeserializer: CirTreeClassConstructorDeserializer,
) {
    operator fun invoke(
        classEntry: ClassesToProcess.ClassEntry, classesToProcess: ClassesToProcess, typeResolver: CirTypeResolver
    ): CirTreeClass {
        val classTypeResolver = typeResolver.create(classEntry)
        val classId = classEntry.classId
        val className = classId.relativeNameSegments.last()

        val clazz: KmClass?
        val isEnumEntry: Boolean

        val cirClass = when (classEntry) {
            is ClassesToProcess.ClassEntry.RegularClassEntry -> {
                clazz = classEntry.clazz
                isEnumEntry = clazz.kind == ClassKind.ENUM_ENTRY
                CirDeserializers.clazz(className, clazz, classTypeResolver)
            }
            is ClassesToProcess.ClassEntry.EnumEntry -> {
                clazz = null
                isEnumEntry = true

                CirDeserializers.defaultEnumEntry(
                    name = className,
                    annotations = classEntry.annotations,
                    enumClassId = classEntry.enumClassId,
                    hasEnumEntries = classEntry.enumClass.hasEnumEntries,
                    typeResolver = classTypeResolver
                )
            }
        }

        val constructors = clazz?.constructors
            ?.takeIf { !isEnumEntry }
            ?.map { constructor -> constructorDeserializer(constructor, cirClass, classTypeResolver) }
            .orEmpty()

        val properties = clazz?.properties?.mapNotNull { property -> propertyDeserializer(property, cirClass, classTypeResolver) }

        val functions = clazz?.functions?.mapNotNull { function -> functionDeserializer(function, cirClass, classTypeResolver) }

        val classes = classesToProcess.classesInScope(classId).map { nestedClassEntry ->
            this(nestedClassEntry, classesToProcess, classTypeResolver)
        }

        return CirTreeClass(
            id = classId,
            clazz = cirClass,
            properties = properties.orEmpty(),
            functions = functions.orEmpty(),
            constructors = constructors,
            classes = classes
        )
    }
}
