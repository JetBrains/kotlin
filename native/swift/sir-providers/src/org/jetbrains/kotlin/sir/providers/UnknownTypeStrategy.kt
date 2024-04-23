/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers

/**
 * What should we do when encounter unknown type (e.g. a type from unknown klib)
 */
public enum class UnknownTypeStrategy {
    /**
     * Return a [SirTypeProvider.TranslationResponse.Unknown] result
     */
    Fail,

    /**
     * Return a [SirTypeProvider.TranslationResponse.Success] with a special type.
     */
    SpecialType
}