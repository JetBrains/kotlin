/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference

// TODO [beresnev] get rid of
internal fun PsiElement.resolveToGetDri(): PsiElement? =
    reference?.resolveToDocumentedElement()

/**
 * Resolves a JavaDoc reference (used for `{@link}`, `{@linkplain}` and `@see` tags).
 *
*/
internal fun PsiReference.resolveToDocumentedElement(): PsiElement? {
    /**
    * [PsiReference.resolve] returns `null` for an ambiguous reference. This happens, in particular,
    * when a method overrides a method from the classpath that is also declared in several inherited
    * interfaces: e.g. `LinkedList#getFirst` is also declared in `Deque` and `SequencedCollection`, so a
    * `{@link #getFirst()}` inside a `LinkedList` subclass resolves to more than one candidate.
    *
    * Dokka links always point to a single declaration, so in that case we just take the first candidate.
    * It is the declaration from the nearest scope (PSI lists a class's own declarations before inherited
    * ones), so for an override of a classpath method it is the override itself — the natural default, since
    * the parent can always be linked explicitly via its class name.
    *
    * See https://github.com/Kotlin/dokka/issues/4543
    */
    resolve()?.let { return it }
    if (this is PsiPolyVariantReference) {
        return multiResolve(false).firstOrNull()?.element
    }
    return null
}
