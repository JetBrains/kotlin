/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.*

internal class ObjCExportNamerProxy(
        private val locator: ObjCExportDeclarationLocator,
) : ObjCExportNamer {

    override fun getFileClassName(file: SourceFile): ObjCExportNamer.ClassOrProtocolName =
            locator.findNamerForSourceFile(file).getFileClassName(file)

    override fun getClassOrProtocolName(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName =
            locator.findNamerForDeclaration(descriptor).getClassOrProtocolName(descriptor)

    override fun getSelector(method: FunctionDescriptor): String =
            locator.findNamerForDeclaration(method).getSelector(method)

    override fun getParameterName(parameter: ParameterDescriptor): String =
            locator.findNamerForDeclaration(parameter).getParameterName(parameter)

    override fun getSwiftName(method: FunctionDescriptor): String =
            locator.findNamerForDeclaration(method).getSwiftName(method)

    override fun getPropertyName(property: PropertyDescriptor): ObjCExportNamer.PropertyName =
            locator.findNamerForDeclaration(property).getPropertyName(property)

    override fun getObjectInstanceSelector(descriptor: ClassDescriptor): String =
            locator.findNamerForDeclaration(descriptor).getObjectInstanceSelector(descriptor)

    override fun getEnumEntrySelector(descriptor: ClassDescriptor): String =
            locator.findNamerForDeclaration(descriptor).getEnumEntrySelector(descriptor)

    override fun getEnumEntrySwiftName(descriptor: ClassDescriptor): String =
            locator.findNamerForDeclaration(descriptor).getEnumEntrySwiftName(descriptor)

    override fun getEnumValuesSelector(descriptor: FunctionDescriptor): String =
            locator.findNamerForDeclaration(descriptor).getEnumValuesSelector(descriptor)

    override fun getTypeParameterName(typeParameterDescriptor: TypeParameterDescriptor): String =
            locator.findNamerForDeclaration(typeParameterDescriptor).getTypeParameterName(typeParameterDescriptor)

    override fun getObjectPropertySelector(descriptor: ClassDescriptor): String =
            locator.findNamerForDeclaration(descriptor).getObjectPropertySelector(descriptor)

    override fun getCompanionObjectPropertySelector(descriptor: ClassDescriptor): String =
            locator.findNamerForDeclaration(descriptor).getCompanionObjectPropertySelector(descriptor)
}