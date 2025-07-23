/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

class SirEnumStub(
    override val origin: SirOrigin,
    override val name: String,
) : SirEnum(), SirMutableDeclarationContainer, SirDeclaration {
    override val visibility: SirVisibility
        get() = SirVisibility.PUBLIC
    override val documentation: String?
        get() = null
    override lateinit var parent: SirDeclarationParent
    override val attributes: List<SirAttribute>
        get() = emptyList()
    override val declarations: MutableList<SirDeclaration> = mutableListOf()
    override val cases: List<SirEnumCase>
        get() = emptyList()
}
