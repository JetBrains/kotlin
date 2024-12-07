/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

/**
 * ObjC method consists of 3 parts, each part needs to be mangled
 * - selectors [buildMangledSelectors]
 * - parameters [buildMangledParameters]
 * - swift_name attribute [buildMangledSwiftNameMethodAttribute]
 */
internal class ObjCMethodMangler {

    private val mangledMethods = mutableMapOf<String, ObjCMemberDetails>()

    fun mangle(member: ObjCExportStub, containingStub: ObjCExportStub): ObjCExportStub {
        if (!member.isSwiftNameMethod()) return member
        if (!contains(member)) {
            cacheMember(member)
            return member
        } else {
            val key = getMemberKey(member as ObjCMethod)
            val attribute = mangledMethods[key] ?: error("No cached item for $member")
            val mangledAttribute = attribute.mangleAttribute()
            val cloned = member.copy(
                buildMangledSelectors(mangledAttribute),
                buildMangledParameters(mangledAttribute),
                buildMangledSwiftNameMethodAttribute(mangledAttribute, containingStub)
            )
            mangledMethods[key] = mangledAttribute
            return cloned
        }
    }

    private fun contains(member: ObjCExportStub): Boolean {
        if (!member.isSwiftNameMethod()) return false
        return mangledMethods[getMemberKey(member as ObjCMethod)] != null
    }

    private fun cacheMember(member: ObjCExportStub) {
        val memberKey = getMemberKey(member as ObjCMethod)
        val swiftNameAttr = getSwiftNameAttribute(member)
        mangledMethods[memberKey] = parseSwiftMethodNameAttribute(swiftNameAttr, member.returnType == ObjCInstanceType)
    }
}