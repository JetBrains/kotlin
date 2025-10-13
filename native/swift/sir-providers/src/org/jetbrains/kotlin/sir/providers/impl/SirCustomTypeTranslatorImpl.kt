/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.providers.SirCustomTypeTranslator
import org.jetbrains.kotlin.sir.providers.SirSession

public class SirCustomTypeTranslatorImpl(
    private val session: SirSession
) : SirCustomTypeTranslator {
    // These classes already have ObjC counterparts assigned statically in ObjC Export.
    private val supportedFqNames: List<FqName> =
        listOf(
            FqNames.set,
            FqNames.mutableSet,
            FqNames.map,
            FqNames.mutableList,
            FqNames.list,
            FqNames.mutableMap,
            FqNames.string.toSafe()
        )

    public override fun isFqNameSupported(fqName: FqName): Boolean {
        return supportedFqNames.contains(fqName)
    }

}
