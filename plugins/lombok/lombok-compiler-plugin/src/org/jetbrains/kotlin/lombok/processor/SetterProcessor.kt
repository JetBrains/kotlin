/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.lombok.config.Accessors
import org.jetbrains.kotlin.lombok.config.Data
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.config.Setter
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

class SetterProcessor(private val config: LombokConfig) : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, jClass: JavaClassImpl): Parts {
        //lombok doesn't generate setters for enums
        if (classDescriptor.kind == ClassKind.ENUM_CLASS) return Parts.Empty

        val globalAccessors = Accessors.get(classDescriptor, config)
        val clSetter = Setter.getOrNull(classDescriptor) ?: Data.getOrNull(classDescriptor)?.asSetter()

        val functions = classDescriptor
            .getJavaFields()
            .collectWithNotNull { field -> Setter.getOrNull(field) ?: clSetter.takeIf { field.isVar } }
            .mapNotNull { (field, setter) -> createSetter(classDescriptor, field, setter, globalAccessors) }
        return Parts(functions)
    }

    private fun createSetter(
        classDescriptor: ClassDescriptor,
        field: PropertyDescriptor,
        getter: Setter,
        globalAccessors: Accessors
    ): SimpleFunctionDescriptor? {
        val accessors = Accessors.getIfAnnotated(field, config) ?: globalAccessors

        val functionName =
            if (accessors.fluent) {
                field.name.identifier
            } else {
                "set" + toPreparedBase(field.name.identifier)
            }

        val returnType = if (accessors.chain) classDescriptor.defaultType else classDescriptor.builtIns.unitType

        return classDescriptor.createFunction(
            Name.identifier(functionName),
            listOf(ValueParameter(field.name, field.type)),
            returnType,
            visibility = getter.visibility
        )
    }
}
