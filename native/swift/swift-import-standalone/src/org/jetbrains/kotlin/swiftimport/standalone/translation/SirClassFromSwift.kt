/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.translation

import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirVisibility

/**
 * A Swift class with members that are added after the class itself is added to its parent:
 * the bridges of the members refer to the class by its fully qualified name, so the class must have a parent by then.
 *
 * Swift classes cannot be subclassed in Kotlin, so the class is final.
 */
internal class SirClassFromSwift(
    override val name: String,
    override val origin: SirOrigin,
) : SirClass() {
    override val visibility: SirVisibility get() = SirVisibility.PUBLIC
    override val documentation: String? get() = null
    override val attributes: List<SirAttribute> get() = emptyList()
    override val declarations: MutableList<SirDeclaration> = mutableListOf()
    override val superClass: SirNominalType? get() = null
    override val protocols: List<SirProtocol> get() = emptyList()
    override val bridges: List<SirBridge> get() = emptyList()
    override val modality: SirModality get() = SirModality.FINAL
    override lateinit var parent: SirDeclarationParent

    fun <T : SirDeclaration> addMember(producer: () -> T): T = producer().also { member ->
        member.parent = this
        declarations += member
    }
}
