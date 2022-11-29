/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.gradle

open class AssignmentExtension {
    internal val myAnnotations = mutableListOf<String>()

    open fun annotation(fqName: String) {
        myAnnotations.add(fqName)
    }

    open fun annotations(fqNames: List<String>) {
        myAnnotations.addAll(fqNames)
    }

    open fun annotations(vararg fqNames: String) {
        myAnnotations.addAll(fqNames)
    }
}
