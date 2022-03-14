// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;
import org.jetbrains.kotlin.js.util.AstUtil;


/**
 * Represents a JavaScript block in the global scope.
 */
class JsVirtualBlock(statements: List<JsStatement> = emptyList()) : JsBlock(statements) {
    override fun isVirtualBlock(): Boolean {
        return true
    }

    override fun deepCopy(): JsVirtualBlock  {
        val globalBlockCopy = JsVirtualBlock()
        val statementscopy = AstUtil.deepCopy(statements);
        globalBlockCopy.statements.addAll(statementscopy);
        return globalBlockCopy.withMetadataFrom(this);
    }
}