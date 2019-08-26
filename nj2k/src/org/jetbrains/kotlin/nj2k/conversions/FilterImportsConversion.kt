/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ImportStorage
import org.jetbrains.kotlin.nj2k.tree.JKImportList
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement


class FilterImportsConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKImportList) return recurse(element)
        element.imports = element.imports.filter { import ->
            ImportStorage.isImportNeeded(import.name.value)
        }
        return recurse(element)
    }
}