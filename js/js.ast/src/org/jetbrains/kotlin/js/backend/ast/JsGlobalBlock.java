// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.kotlin.js.util.AstUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a JavaScript block in the global scope.
 */
public class JsGlobalBlock extends JsBlock {
    @Override
    public boolean isGlobalBlock() {
        return true;
    }

    @NotNull
    @Override
    public JsGlobalBlock deepCopy() {
        JsGlobalBlock globalBlockCopy = new JsGlobalBlock();
        List<JsStatement> statementscopy = AstUtil.deepCopy(getStatements());
        globalBlockCopy.getStatements().addAll(statementscopy);
        return globalBlockCopy.withMetadataFrom(this);
    }
}
