// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;
import org.jetbrains.kotlin.js.util.AstUtil;


/**
 * Represents a JavaScript block which could not be rendered into a material one.
 */
class JsCompositeBlock : JsBlock {
    constructor() : super()
    constructor(statement: JsStatement) : super(statement)
    constructor(statements: List<JsStatement>) : super(statements)

    override fun isTransparent(): Boolean {
        return true
    }

    override fun deepCopy(): JsCompositeBlock {
        val globalBlockCopy = JsCompositeBlock()
        val statementscopy = AstUtil.deepCopy(statements);
        globalBlockCopy.statements.addAll(statementscopy);
        return globalBlockCopy.withMetadataFrom(this);
    }
}
