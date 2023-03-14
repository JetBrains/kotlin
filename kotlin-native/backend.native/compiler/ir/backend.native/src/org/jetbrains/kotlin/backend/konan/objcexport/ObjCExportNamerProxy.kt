/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.*

internal class ObjCExportNamerProxy(
        private val namerProvider: ObjCNamerProvider,
        private val headerIdProvider: ObjCExportHeaderIdProvider
) : ObjCExportNamer {

    override fun getFileClassName(file: SourceFile): ObjCExportNamer.ClassOrProtocolName =
            namerProvider.getNamer(headerIdProvider.getHeaderId(file)).getFileClassName(file)

    override fun getClassOrProtocolName(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName =
            namerProvider.getNamer(headerIdProvider.getHeaderId(descriptor)).getClassOrProtocolName(descriptor)

    override fun getSelector(method: FunctionDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(method)).getSelector(method)

    override fun getParameterName(parameter: ParameterDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(parameter)).getParameterName(parameter)

    override fun getSwiftName(method: FunctionDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(method)).getSwiftName(method)

    override fun getPropertyName(property: PropertyDescriptor): ObjCExportNamer.PropertyName =
            namerProvider.getNamer(headerIdProvider.getHeaderId(property)).getPropertyName(property)

    override fun getObjectInstanceSelector(descriptor: ClassDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(descriptor)).getObjectInstanceSelector(descriptor)

    override fun getEnumEntrySelector(descriptor: ClassDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(descriptor)).getEnumEntrySelector(descriptor)

    override fun getEnumEntrySwiftName(descriptor: ClassDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(descriptor)).getEnumEntrySwiftName(descriptor)

    override fun getEnumValuesSelector(descriptor: FunctionDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(descriptor)).getEnumValuesSelector(descriptor)

    override fun getTypeParameterName(typeParameterDescriptor: TypeParameterDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(typeParameterDescriptor)).getTypeParameterName(typeParameterDescriptor)

    override fun getObjectPropertySelector(descriptor: ClassDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(descriptor)).getObjectPropertySelector(descriptor)

    override fun getCompanionObjectPropertySelector(descriptor: ClassDescriptor): String =
            namerProvider.getNamer(headerIdProvider.getHeaderId(descriptor)).getCompanionObjectPropertySelector(descriptor)
}