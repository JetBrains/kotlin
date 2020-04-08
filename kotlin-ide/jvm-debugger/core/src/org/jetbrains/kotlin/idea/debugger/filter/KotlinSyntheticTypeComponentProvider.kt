/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.filter

import com.sun.jdi.Method
import com.sun.jdi.TypeComponent
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_IMPL_NAME_SUFFIX
import org.jetbrains.kotlin.idea.debugger.isInKotlinSources

class KotlinSyntheticTypeComponentProvider : KotlinSyntheticTypeComponentProviderBase() {
    override fun isNotSynthetic(typeComponent: TypeComponent?): Boolean {
        if (typeComponent is Method && typeComponent.name().endsWith(SUSPEND_IMPL_NAME_SUFFIX)) {
            if (typeComponent.location()?.isInKotlinSources() == true) {
                val containingClass = typeComponent.declaringType()
                if (typeComponent.argumentTypeNames().firstOrNull() == containingClass.name()) {
                    // Suspend wrapper for open method
                    return true
                }
            }
        }

        return super.isNotSynthetic(typeComponent)
    }
}