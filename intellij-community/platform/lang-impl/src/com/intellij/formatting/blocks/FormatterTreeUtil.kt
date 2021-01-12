// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.blocks

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import org.jetbrains.annotations.ApiStatus

@Deprecated("Bad API (non-descriptive name, cryptic behaviour, rarely needed). Implement your own using `TreeUtil#skipElementsBack`.")
@ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
@Suppress("DEPRECATION")
fun ASTNode.prev(): ASTNode? {
  var prev = treePrev
  while (prev != null && prev.elementType == TokenType.WHITE_SPACE) {
    prev = prev.treePrev
  }
  if (prev != null) return prev
  return if (treeParent != null) treeParent.prev() else null
}