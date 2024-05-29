/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.sir.util.name

public class PackageFlatteningSirEnumGenerator(
    private val sirSession: SirSession,
    private val enumGenerator: SirEnumGenerator,
    private val moduleForEnums: SirModule
) : SirEnumGenerator {
    private val processedDeclarations: MutableSet<SirEnum> = mutableSetOf()

    override fun FqName.sirPackageEnum(): SirEnum = with(enumGenerator) { this@sirPackageEnum.sirPackageEnum() }
        .also {
            if (!processedDeclarations.contains(it)) {
                processedDeclarations.add(it)
                with(sirSession) { it.trampolineDeclarations().forEach { moduleForEnums.addChild { it } } }
            }
        }
}
