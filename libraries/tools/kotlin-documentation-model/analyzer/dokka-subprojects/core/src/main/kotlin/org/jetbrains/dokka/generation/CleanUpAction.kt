/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.generation

/** Runs before generation exits. This action is run even if generation fails. */
public fun interface CleanUpAction : () -> Unit
