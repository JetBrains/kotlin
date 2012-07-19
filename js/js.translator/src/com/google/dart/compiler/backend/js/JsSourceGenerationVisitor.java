// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsContext;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsProgramFragment;
import com.google.dart.compiler.util.TextOutput;

/**
 * Generates JavaScript source from an AST.
 */
public class JsSourceGenerationVisitor extends JsToStringGenerationVisitor {
    public JsSourceGenerationVisitor(TextOutput out) {
        super(out);
    }

    @Override
    public boolean visit(JsProgram program, JsContext ctx) {
        return true;
    }

    @Override
    public boolean visit(JsProgramFragment x, JsContext ctx) {
        // Descend naturally.
        return true;
    }

    @Override
    public boolean visit(JsBlock x, JsContext ctx) {
        printJsBlock(x, false, true);
        return false;
    }
}
