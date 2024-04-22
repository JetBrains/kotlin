/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.sir.tree.generator.BASE_PACKAGE

class Element(name: String, override val propertyName: String) : AbstractElement<Element, Field, Implementation>(name) {

    override var visitorParameterName: String = safeDecapitalizedName

    override val namePrefix: String
        get() = "Sir"

    override val packageName: String
        get() = BASE_PACKAGE
}