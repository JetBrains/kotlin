/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ImportStorage
import org.jetbrains.kotlin.nj2k.tree.JKImportList
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement


class FilterImportsConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element is JKImportList) {
            for (import in element.imports) {
                if (!ImportStorage.isImportNeeded(import.name.value)) {
                    element.imports -= import
                }
            }
        }
        return recurse(element)
    }
}