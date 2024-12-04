/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import com.intellij.psi.PsiElement

import org.jetbrains.kotlin.name.Name

sealed class ObjCExportStubOrigin {

    /**
     * The original 'Kotlin' name of the entity that is associated with this stub
     */
    abstract val name: Name?

    /**
     * The original 'Kotlin documentation' of the associated with this stub
     */
    abstract val kdoc: String?

    /**
     * The stub was produced from Kotlin sources
     */
    data class Source(override val name: Name?, override val kdoc: String?, val psi: PsiElement?) : ObjCExportStubOrigin()

    /**
     * The stub was produced from a compiled binary (e.g. when translating a dependency inside fleet).
     * Note: On CLI invocations, the ObjC export will only operate on binaries.
     */
    data class Binary(override val name: Name?, override val kdoc: String?) : ObjCExportStubOrigin()
}
