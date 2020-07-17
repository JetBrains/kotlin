/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree

import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface Modifier {
    val text: String
}

enum class OtherModifier(override val text: String) : Modifier {
    OVERRIDE("override"),
    ACTUAL("actual"),
    ANNOTATION("annotation"),
    COMPANION("companion"),
    CONST("const"),
    CROSSINLINE("crossinline"),
    DATA("data"),
    EXPECT("expect"),
    EXTERNAL("external"),
    INFIX("infix"),
    INLINE("inline"),
    INNER("inner"),
    LATEINIT("lateinit"),
    NOINLINE("noinline"),
    OPERATOR("operator"),
    OUT("out"),
    REIFIED("reified"),
    SEALED("sealed"),
    SUSPEND("suspend"),
    TAILREC("tailrec"),
    VARARG("vararg"),
    FUN("fun"),

    NATIVE("native"),
    STATIC("static"),
    STRICTFP("strictfp"),
    SYNCHRONIZED("synchronized"),
    TRANSIENT("transient"),
    VOLATILE("volatile")
}

sealed class JKModifierElement : JKTreeElement()


interface JKOtherModifiersOwner : JKModifiersListOwner {
    var otherModifierElements: List<JKOtherModifierElement>
}

class JKMutabilityModifierElement(var mutability: Mutability) : JKModifierElement() {
    override fun accept(visitor: JKVisitor) = visitor.visitMutabilityModifierElement(this)
}

class JKModalityModifierElement(var modality: Modality) : JKModifierElement() {
    override fun accept(visitor: JKVisitor) = visitor.visitModalityModifierElement(this)
}

class JKVisibilityModifierElement(var visibility: Visibility) : JKModifierElement() {
    override fun accept(visitor: JKVisitor) = visitor.visitVisibilityModifierElement(this)
}

class JKOtherModifierElement(var otherModifier: OtherModifier) : JKModifierElement() {
    override fun accept(visitor: JKVisitor) = visitor.visitOtherModifierElement(this)
}


interface JKVisibilityOwner : JKModifiersListOwner {
    val visibilityElement: JKVisibilityModifierElement
}

enum class Visibility(override val text: String) : Modifier {
    PUBLIC("public"),
    INTERNAL("internal"),
    PROTECTED("protected"),
    PRIVATE("private")
}

interface JKModalityOwner : JKModifiersListOwner {
    val modalityElement: JKModalityModifierElement
}

enum class Modality(override val text: String) : Modifier {
    OPEN("open"),
    FINAL("final"),
    ABSTRACT("abstract"),
}

interface JKMutabilityOwner : JKModifiersListOwner {
    val mutabilityElement: JKMutabilityModifierElement
}

enum class Mutability(override val text: String) : Modifier {
    MUTABLE("var"),
    IMMUTABLE("val"),
    UNKNOWN("var")
}

interface JKModifiersListOwner : JKFormattingOwner

fun JKOtherModifiersOwner.elementByModifier(modifier: OtherModifier): JKOtherModifierElement? =
    otherModifierElements.firstOrNull { it.otherModifier == modifier }

fun JKOtherModifiersOwner.hasOtherModifier(modifier: OtherModifier): Boolean =
    otherModifierElements.any { it.otherModifier == modifier }

var JKVisibilityOwner.visibility: Visibility
    get() = visibilityElement.visibility
    set(value) {
        visibilityElement.visibility = value
    }

var JKMutabilityOwner.mutability: Mutability
    get() = mutabilityElement.mutability
    set(value) {
        mutabilityElement.mutability = value
    }

var JKModalityOwner.modality: Modality
    get() = modalityElement.modality
    set(value) {
        modalityElement.modality = value
    }


val JKModifierElement.modifier: Modifier
    get() = when (this) {
        is JKMutabilityModifierElement -> mutability
        is JKModalityModifierElement -> modality
        is JKVisibilityModifierElement -> visibility
        is JKOtherModifierElement -> otherModifier
    }


inline fun JKModifiersListOwner.forEachModifier(action: (JKModifierElement) -> Unit) {
    safeAs<JKVisibilityOwner>()?.visibilityElement?.let(action)
    safeAs<JKModalityOwner>()?.modalityElement?.let(action)
    safeAs<JKOtherModifiersOwner>()?.otherModifierElements?.forEach(action)
    safeAs<JKMutabilityOwner>()?.mutabilityElement?.let(action)
}

