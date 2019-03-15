/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi.asm

import org.jetbrains.org.objectweb.asm.ClassVisitor

internal class FilterInnerClassesVisitor(
    private val innerClassesToFilter: Set<String>,
    api: Int,
    cv: ClassVisitor
) : ClassVisitor(api, cv) {
    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        if (name in innerClassesToFilter) return

        super.visitInnerClass(name, outerName, innerName, access)
    }
}