/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal class SharedObjCExportNamer(
        private val sharedState: ObjCExportSharedState,
) : ObjCExportNamer {

    private fun findDelegateFor(descriptor: DeclarationDescriptor): ObjCExportNamer =
            sharedState.findExportNamerFor(descriptor.module)

    private fun findStdlibDelegate(): ObjCExportNamer =
            sharedState.findExportNamerForStdlib()

    override val stdlibTopLevelPrefix: String by lazy {
        sharedState.findExportNamerForStdlib().stdlibTopLevelPrefix
    }

    override fun getClassOrProtocolName(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName =
            findDelegateFor(descriptor).getClassOrProtocolName(descriptor)

    override fun getSelector(method: FunctionDescriptor): String =
            findDelegateFor(method).getSelector(method)

    override fun getSwiftName(method: FunctionDescriptor): String =
            findDelegateFor(method).getSwiftName(method)

    override fun getPropertyName(property: PropertyDescriptor): ObjCExportNamer.PropertyName =
            findDelegateFor(property).getPropertyName(property)

    override fun getObjectInstanceSelector(descriptor: ClassDescriptor): String =
            findDelegateFor(descriptor).getObjectInstanceSelector(descriptor)

    override fun getEnumEntrySelector(descriptor: ClassDescriptor): String =
            findDelegateFor(descriptor).getEnumEntrySelector(descriptor)

    override fun getEnumEntrySwiftName(descriptor: ClassDescriptor): String =
            findDelegateFor(descriptor).getEnumEntrySwiftName(descriptor)

    override fun getEnumValuesSelector(descriptor: FunctionDescriptor): String =
            findDelegateFor(descriptor).getEnumValuesSelector(descriptor)

    override fun getTypeParameterName(typeParameterDescriptor: TypeParameterDescriptor): String =
            findDelegateFor(typeParameterDescriptor).getTypeParameterName(typeParameterDescriptor)

    override fun numberBoxName(classId: ClassId): ObjCExportNamer.ClassOrProtocolName =
            findStdlibDelegate().numberBoxName(classId)

    override fun getObjectPropertySelector(descriptor: ClassDescriptor): String =
            findDelegateFor(descriptor).getObjectPropertySelector(descriptor)

    override fun getCompanionObjectPropertySelector(descriptor: ClassDescriptor): String =
            findDelegateFor(descriptor).getCompanionObjectPropertySelector(descriptor)

    override val kotlinAnyName: ObjCExportNamer.ClassOrProtocolName
        get() = findStdlibDelegate().kotlinAnyName
    override val mutableSetName: ObjCExportNamer.ClassOrProtocolName
        get() = findStdlibDelegate().mutableSetName
    override val mutableMapName: ObjCExportNamer.ClassOrProtocolName
        get() = findStdlibDelegate().mutableMapName
    override val kotlinNumberName: ObjCExportNamer.ClassOrProtocolName
        get() = findStdlibDelegate().kotlinNumberName
}