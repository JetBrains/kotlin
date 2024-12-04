/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.processor

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.lombok.config.*
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Accessors
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Setter
import org.jetbrains.kotlin.lombok.config.LombokAnnotations.Data
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

class SetterProcessor(private val config: LombokConfig) : Processor {

    override fun contribute(classDescriptor: ClassDescriptor, partsBuilder: SyntheticPartsBuilder, c: LazyJavaResolverContext) {
        //lombok doesn't generate setters for enums
        if (classDescriptor.kind == ClassKind.ENUM_CLASS) return

        val globalAccessors = Accessors.get(classDescriptor, config)
        val clSetter = Setter.getOrNull(classDescriptor) ?: Data.getOrNull(classDescriptor)?.asSetter()

        classDescriptor
            .getJavaFields()
            .collectWithNotNull { field -> Setter.getOrNull(field) ?: clSetter.takeIf { field.isVar } }
            .mapNotNull { (field, setter) -> createSetter(classDescriptor, field, setter, globalAccessors) }
            .forEach(partsBuilder::addMethod)
    }

    private fun createSetter(
        classDescriptor: ClassDescriptor,
        field: PropertyDescriptor,
        getter: Setter,
        globalAccessors: Accessors
    ): SimpleFunctionDescriptor? {
        if (getter.visibility == AccessLevel.NONE) return null

        val accessors = Accessors.getIfAnnotated(field, config) ?: globalAccessors
        return field.toAccessorBaseName(accessors)?.let { propertyName ->
            val functionName =
                if (accessors.fluent) propertyName
                else AccessorNames.SET + propertyName.capitalize()

            val returnType = if (accessors.chain) classDescriptor.defaultType else classDescriptor.builtIns.unitType

            classDescriptor.createFunction(
                Name.identifier(functionName),
                listOf(LombokValueParameter(field.name, field.type)),
                returnType,
                visibility = getter.visibility.toDescriptorVisibility()
            )
        }
    }
}
