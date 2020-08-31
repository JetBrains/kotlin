/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode


/**
 * References a member that can be used as a type
 */
interface IdentifiableMember {
    val name: String
    fun imports(): Set<String> = emptySet()

}
