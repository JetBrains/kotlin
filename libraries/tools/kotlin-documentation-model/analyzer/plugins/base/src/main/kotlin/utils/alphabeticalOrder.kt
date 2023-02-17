package org.jetbrains.dokka.base.utils


/**
 * Canonical alphabetical order to sort named elements
 */
internal val canonicalAlphabeticalOrder: Comparator<in String> = String.CASE_INSENSITIVE_ORDER.thenBy { it }