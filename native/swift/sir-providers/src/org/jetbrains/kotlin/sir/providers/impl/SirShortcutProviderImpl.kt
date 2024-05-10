/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.providers.SirShortcutProvider

public class SirShortcutProviderImpl(
    private val commonPrefix: FqName?,
) : SirShortcutProvider {
    override fun FqName.hasShortcut(): Boolean {
        if (commonPrefix == null) return false

        return this.startsWith(commonPrefix)
    }
}