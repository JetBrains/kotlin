/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.kotlin.internal.KotlinUElementWithComments

abstract class KotlinAbstractUElement(
    givenParent: UElement?,
    baseResolveProviderServiceSupplier: BaseResolveProviderServiceSupplier? = null,
) : KotlinUElementWithComments {

    protected val languagePlugin: UastLanguagePlugin? by lz {
        psi?.let { UastFacade.findPlugin(it) }
    }

    protected val baseResolveProviderService: BaseKotlinUastResolveProviderService by lz {
        baseResolveProviderServiceSupplier?.get()
            ?: psi?.project?.let { ServiceManager.getService(it, BaseKotlinUastResolveProviderService::class.java) }
            ?: error("${BaseKotlinUastResolveProviderService::class.java.name} is not available")
    }

    final override val uastParent: UElement? by lz {
        givenParent ?: convertParent()
    }

    protected open fun convertParent(): UElement? {
        return baseResolveProviderService.convertParent(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UElement) {
            return false
        }

        return this.psi == other.psi
    }

    override fun hashCode(): Int {
        return psi?.hashCode() ?: 0
    }
}
