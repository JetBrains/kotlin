/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.extractSerializedKdocString
import org.jetbrains.kotlin.backend.common.serialization.metadata.findKDocString
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

fun ObjCExportStubOrigin(descriptor: DeclarationDescriptor?): ObjCExportStubOrigin? {
    if (descriptor == null) return null

    if (descriptor is DeserializedDescriptor) {
        return ObjCExportStubOrigin.Binary(descriptor.name, descriptor.extractSerializedKdocString())
    }

    if (descriptor is DeclarationDescriptorWithSource) {
        return ObjCExportStubOrigin.Source(descriptor.name, descriptor.findKDocString(), (descriptor.source as? PsiSourceElement)?.psi)
    }

    /*
    This case is somewhat unexpected/esoteric:
    We expect the descriptor to either implement `DeserializedDescriptor` or `DeclarationDescriptorWithSource` (or both)
    The returned 'Binary' is a defensive measure.
     */
    return ObjCExportStubOrigin.Binary(descriptor.name, kdoc = null)
}


fun ObjCProtocolImpl(
    name: String,
    descriptor: ClassDescriptor,
    superProtocols: List<String>,
    members: List<ObjCExportStub>,
    attributes: List<String> = emptyList(),
    comment: ObjCComment? = null,
) = ObjCProtocolImpl(
    name = name,
    comment = comment,
    origin = ObjCExportStubOrigin(descriptor),
    attributes = attributes,
    superProtocols = superProtocols,
    members = members
)

fun ObjCInterfaceImpl(
    name: String,
    generics: List<ObjCGenericTypeDeclaration> = emptyList(),
    descriptor: ClassDescriptor? = null,
    superClass: String? = null,
    superClassGenerics: List<ObjCNonNullReferenceType> = emptyList(),
    superProtocols: List<String> = emptyList(),
    categoryName: String? = null,
    members: List<ObjCExportStub> = emptyList(),
    attributes: List<String> = emptyList(),
    comment: ObjCComment? = null,
) = ObjCInterfaceImpl(
    name = name,
    comment = comment,
    origin = ObjCExportStubOrigin(descriptor),
    attributes = attributes,
    superProtocols = superProtocols,
    members = members,
    categoryName = categoryName,
    generics = generics,
    superClass = superClass,
    superClassGenerics = superClassGenerics
)

fun ObjCMethod(
    descriptor: DeclarationDescriptor?,
    isInstanceMethod: Boolean,
    returnType: ObjCType,
    selectors: List<String>,
    parameters: List<ObjCParameter>,
    attributes: List<String>,
    comment: ObjCComment? = null,
) = ObjCMethod(
    comment = comment,
    origin = ObjCExportStubOrigin(descriptor),
    isInstanceMethod = isInstanceMethod,
    returnType = returnType,
    selectors = selectors,
    parameters = parameters,
    attributes = attributes
)

fun ObjCParameter(
    name: String,
    descriptor: ParameterDescriptor?,
    type: ObjCType,
) = ObjCParameter(
    name = name,
    origin = ObjCExportStubOrigin(descriptor),
    type = type,
    todo = null
)

fun ObjCProperty(
    name: String,
    descriptor: DeclarationDescriptorWithSource?,
    type: ObjCType,
    propertyAttributes: List<String>,
    setterName: String? = null,
    getterName: String? = null,
    declarationAttributes: List<String> = emptyList(),
    comment: ObjCComment? = null,
) = ObjCProperty(
    name = name,
    comment = comment,
    origin = ObjCExportStubOrigin(descriptor),
    type = type,
    propertyAttributes = propertyAttributes,
    setterName = setterName,
    getterName = getterName,
    declarationAttributes = declarationAttributes
)