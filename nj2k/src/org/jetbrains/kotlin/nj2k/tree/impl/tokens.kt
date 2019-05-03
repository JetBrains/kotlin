/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.tree.impl

import org.jetbrains.kotlin.nj2k.tree.JKCommentElement
import org.jetbrains.kotlin.nj2k.tree.JKNonCodeElement
import org.jetbrains.kotlin.nj2k.tree.JKSpaceElement
import org.jetbrains.kotlin.nj2k.tree.JKTokenElement


class JKSpaceElementImpl(override val text: String) : JKSpaceElement

class JKCommentElementImpl(override val text: String) : JKCommentElement

class JKTokenElementImpl(override val text: String) : JKTokenElement {
    override var leftNonCodeElements: List<JKNonCodeElement> = emptyList()
    override var rightNonCodeElements: List<JKNonCodeElement> = emptyList()
}
