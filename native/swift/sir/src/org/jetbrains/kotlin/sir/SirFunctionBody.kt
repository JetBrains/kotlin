/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

/**
 * A body of a SIR function.
 *
 * For now, it is just a list of statements. This is sufficient as bodies are pretty trivial.
 * In the future, it may become more complicated as bridging logic becomes more sophisticated.
 */
class SirFunctionBody(val statements: List<String>)