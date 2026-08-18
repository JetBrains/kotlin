/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.renderers

/**
 * Runs after rendering is complete. If there is an exception thrown during generation, these actions will not be run.
 * For actions that must run even if there is an error, use [org.jetbrains.dokka.generation.CleanUpAction] instead.
 */
public fun interface PostAction : () -> Unit
