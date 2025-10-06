/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute

/**
 * ObjC method consists of 3 parts, each part needs to be mangled
 * - selectors [buildMangledSelectors]
 * - parameters [buildMangledParameters]
 * - swift_name attribute [buildMangledSwiftNameMethodAttribute]
 */
internal class ObjCMethodMangler {

    private val mangledMethods = mutableSetOf<String>()

    fun mangle(member: ObjCExportStub, containingStub: ObjCExportStub): ObjCExportStub {
        if (member is ObjCMethod) {
            var cloned: ObjCMethod = member
            while (true) {
                val key = getMemberKey(cloned)
                if (!mangledMethods.contains(key)) {
                    mangledMethods.add(key)
                    return cloned
                }
                val newSelectors = cloned.selectors.toMutableList()
                val lastIndex = newSelectors.lastIndex
                var lastSelector = newSelectors[lastIndex]
                if (lastSelector.endsWith(":")) {
                    // - (void)foo:; -> -(void)foo_:;
                    // - (void)foo:bar:; -> -(void)foo:bar_:;
                    lastSelector = lastSelector.dropLast(1) + "_:"
                } else {
                    // - (void)foo; -> -(void)foo_;
                    lastSelector += "_"
                }
                newSelectors[lastIndex] = lastSelector
                cloned = member.copy(
                    mangledSelectors = newSelectors,
                    containingStubName = containingStub.name
                )
            }
        }
        return member
    }
}