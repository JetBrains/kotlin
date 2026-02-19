/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.name.ClassId

@InternalKotlinNativeApi
fun objCBaseDeclarations(
    topLevelNamePrefix: String,
    objCNameOfAny: ObjCExportClassOrProtocolName,
    objCNameOfNumber: ObjCExportClassOrProtocolName,
    objCNameOfMutableMap: ObjCExportClassOrProtocolName,
    objCNameOfMutableSet: ObjCExportClassOrProtocolName,
    objCNameForNumberBox: (numberClassId: ClassId) -> ObjCExportClassOrProtocolName
): List<ObjCTopLevel> = buildList {
    add {
        objCInterface(objCNameOfAny, superClass = "NSObject", members = buildList {
            add {
                ObjCMethod(
                    origin = null,
                    comment = null,
                    isInstanceMethod = true,
                    returnType = ObjCInstanceType,
                    selectors = listOf("init"),
                    parameters = emptyList(),
                    attributes = listOf("unavailable")
                )
            }
            add {
                ObjCMethod(
                    origin = null,
                    comment = null,
                    isInstanceMethod = false,
                    returnType = ObjCInstanceType,
                    selectors = listOf("new"),
                    parameters = emptyList(), attributes = listOf("unavailable")
                )
            }
            add {
                ObjCMethod(
                    origin = null,
                    comment = null,
                    isInstanceMethod = false,
                    returnType = ObjCVoidType,
                    selectors = listOf("initialize"),
                    parameters = emptyList(),
                    attributes = listOf("objc_requires_super")
                )
            }
        })
    }

    // TODO: add comment to the header.
    add {
        ObjCInterfaceImpl(
            objCNameOfAny.objCName,
            superProtocols = listOf("NSCopying"),
            categoryName = "${objCNameOfAny.objCName}Copying",
            origin = null,
            comment = null,
            generics = emptyList(),
            attributes = emptyList(),
            members = emptyList(),
            superClass = null,
            superClassGenerics = emptyList()
        )
    }

    // TODO: only if appears
    add {
        val generics = listOf("ObjectType")
        objCInterface(
            objCNameOfMutableSet,
            generics = generics,
            superClass = "NSMutableSet",
            superClassGenerics = generics
        )
    }

    // TODO: only if appears
    add {
        val generics = listOf("KeyType", "ObjectType")
        objCInterface(
            objCNameOfMutableMap,
            generics = generics,
            superClass = "NSMutableDictionary",
            superClassGenerics = generics
        )
    }

    val nsErrorCategoryName = "NSError${topLevelNamePrefix}KotlinException"
    add {
        ObjCInterfaceImpl(
            name = "NSError",
            categoryName = nsErrorCategoryName,
            members = buildList {
                add {
                    ObjCProperty(
                        name = "kotlinException",
                        origin = null,
                        type = ObjCNullableReferenceType(ObjCIdType),
                        propertyAttributes = listOf("readonly"),
                        setterName = null,
                        getterName = null,
                        comment = null,
                        declarationAttributes = emptyList()
                    )
                }
            },
            attributes = emptyList(),
            comment = null,
            origin = null,
            superProtocols = emptyList(),
            generics = emptyList(),
            superClass = null,
            superClassGenerics = emptyList()
        )
    }

    genKotlinNumbers(objCNameOfNumber = objCNameOfNumber, objCNameForNumberBox = objCNameForNumberBox)
}

private fun MutableList<ObjCTopLevel>.genKotlinNumbers(
    objCNameOfNumber: ObjCExportClassOrProtocolName,
    objCNameForNumberBox: (numberClassId: ClassId) -> ObjCExportClassOrProtocolName
) {
    val members = buildList {
        NSNumberKind.entries.forEach {
            add(nsNumberFactory(it, listOf("unavailable")))
        }
        NSNumberKind.entries.forEach {
            add(nsNumberInit(it, listOf("unavailable")))
        }
    }
    add {
        objCInterface(
            objCNameOfNumber,
            superClass = "NSNumber",
            members = members
        )
    }

    NSNumberKind.entries.forEach {
        if (it.mappedKotlinClassId != null) add {
            genKotlinNumber(it.mappedKotlinClassId, it, objCNameOfNumber, objCNameForNumberBox)
        }
    }
}


private fun genKotlinNumber(
    kotlinClassId: ClassId,
    kind: NSNumberKind,
    objCNameOfNumber: ObjCExportClassOrProtocolName,
    objCNameForNumberBox: (numberClassId: ClassId) -> ObjCExportClassOrProtocolName
): ObjCInterface {
    val name = objCNameForNumberBox(kotlinClassId)

    val members = buildList<ObjCExportStub> {
        add { nsNumberFactory(kind) }
        add { nsNumberInit(kind) }
    }
    return objCInterface(
        name,
        superClass = objCNameOfNumber.objCName,
        members = members
    )
}

private fun nsNumberInit(kind: NSNumberKind, attributes: List<String> = emptyList()): ObjCMethod {
    return ObjCMethod(
        origin = null,
        comment = null,
        isInstanceMethod = false,
        returnType = ObjCInstanceType,
        selectors = listOf(kind.factorySelector),
        parameters = listOf(
            ObjCParameter(name = "value", origin = null, type = kind.objCType, todo = null)
        ),
        attributes = attributes
    )
}

private fun nsNumberFactory(kind: NSNumberKind, attributes: List<String> = emptyList()): ObjCMethod {
    return ObjCMethod(
        origin = null,
        comment = null,
        isInstanceMethod = true,
        returnType = ObjCInstanceType,
        selectors = listOf(kind.initSelector),
        parameters = listOf(ObjCParameter("value", null, kind.objCType, todo = null)),
        attributes = attributes
    )
}


private inline fun <T> MutableList<T>.add(producer: () -> T) {
    add(producer())
}

private fun objCInterface(
    name: ObjCExportClassOrProtocolName,
    generics: List<String>,
    superClass: String,
    superClassGenerics: List<String>,
): ObjCInterface = objCInterface(
    name,
    generics = generics.map { ObjCGenericTypeRawDeclaration(it) },
    superClass = superClass,
    superClassGenerics = superClassGenerics.map { ObjCGenericTypeRawUsage(it) }
)

private fun objCInterface(
    name: ObjCExportClassOrProtocolName,
    generics: List<ObjCGenericTypeDeclaration> = emptyList(),
    superClass: String? = null,
    superClassGenerics: List<ObjCNonNullReferenceType> = emptyList(),
    superProtocols: List<String> = emptyList(),
    members: List<ObjCExportStub> = emptyList(),
    attributes: List<String> = emptyList(),
    comment: ObjCComment? = null,
): ObjCInterface = ObjCInterfaceImpl(
    name = name.objCName,
    generics = generics,
    origin = null,
    superClass = superClass,
    superClassGenerics = superClassGenerics,
    superProtocols = superProtocols,
    members = members,
    attributes = attributes.plus(name.toNameAttributes()),
    comment = comment,
    categoryName = null
)

private fun objCProtocol(
    name: ObjCExportClassOrProtocolName,
    superProtocols: List<String>,
    members: List<ObjCExportStub>,
    attributes: List<String> = emptyList(),
    comment: ObjCComment? = null,
): ObjCProtocol = ObjCProtocolImpl(
    name = name.objCName,
    origin = null,
    superProtocols = superProtocols,
    members = members,
    attributes = attributes + name.toNameAttributes(),
    comment = comment
)

