/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty

class ObjCPropertyMangler {

    private val mangledProperties = hashSetOf<String>()

    fun mangle(member: ObjCExportStub, containingStub: ObjCExportStub): ObjCExportStub {
        return if (member.isSwiftNameProperty()) {
            val key = getSwiftNameAttribute(member as ObjCProperty)
            if (mangledProperties.contains(key)) {
                val copy = if (containingStub.isExtensionFacade) {
                    val attr = parseSwiftPropertyNameAttribute(getSwiftNameAttribute(member))
                    member.copy(
                        name = member.name + "_",
                        propertyAttributes = null,
                        declarationAttributes = listOf(buildMangledSwiftNamePropertyAttribute(attr.mangleAttribute()))
                    )
                } else {
                    member.copy(
                        name = member.name,
                        propertyAttributes = "getter=${member.name}_",
                        declarationAttributes = null
                    )
                }
                mangledProperties.add(getSwiftNameAttribute(copy))
                copy
            } else {
                mangledProperties.add(key)
                member
            }
        } else if (member.isSwiftNameMethod()) {
            mangledProperties.add(getSwiftNameAttribute(member as ObjCMethod).replace("()", ""))
            member
        } else {
            // Leave it as it is since it is neither property, nor method
            member
        }
    }
}

private fun buildMangledSwiftNamePropertyAttribute(attribute: ObjCMemberDetails): String {
    return "swift_name(\"${attribute.name + attribute.postfix}\")"
}