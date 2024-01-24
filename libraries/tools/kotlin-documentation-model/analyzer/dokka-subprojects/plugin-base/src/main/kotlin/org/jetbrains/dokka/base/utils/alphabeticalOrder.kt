/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.utils


/**
 * Canonical alphabetical order to sort named elements
 */
internal val canonicalAlphabeticalOrder: Comparator<in String> = String.CASE_INSENSITIVE_ORDER.thenBy { it }
