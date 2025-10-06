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
        return if (member is ObjCProperty) {
            val name = member.name
            if (mangledProperties.contains(name)) {
                val copy = if (containingStub.isExtensionFacade) {
                    member.copy(
                        name = name + "_",
                        propertyAttributes = null,
                        declarationAttributes = null
                    )
                } else {
                    member.copy(
                        name = name,
                        propertyAttributes = "getter=${name}_",
                        declarationAttributes = null
                    )
                }
                copy
            } else {
                mangledProperties.add(name)
                member
            }
        } else if (member is ObjCMethod && member.selectors.size == 1 && member.parameters.isEmpty()) {
            mangledProperties.add(member.selectors[0])
            member
        } else {
            // Leave it as it is since it is neither property, nor method
            member
        }
    }
}