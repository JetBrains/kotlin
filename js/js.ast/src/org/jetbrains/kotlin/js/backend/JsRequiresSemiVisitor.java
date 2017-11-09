// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.*;

/**
 * Determines if a statement at the end of a block requires a semicolon.
 * <p/>
 * For example, the following statements require semicolons:<br>
 * <ul>
 * <li>if (cond);</li>
 * <li>while (cond);</li>
 * </ul>
 * <p/>
 * The following do not require semicolons:<br>
 * <ul>
 * <li>return 1</li>
 * <li>do {} while(true)</li>
 * </ul>
 */
public class JsRequiresSemiVisitor extends JsVisitor {
    private boolean needsSemicolon;

    private JsRequiresSemiVisitor() {
    }

    public static boolean exec(JsStatement lastStatement) {
        JsRequiresSemiVisitor visitor = new JsRequiresSemiVisitor();
        visitor.accept(lastStatement);
        return visitor.needsSemicolon;
    }

    @Override
    public void visitFor(@NotNull JsFor x) {
        if (x.getBody() instanceof JsEmpty) {
            needsSemicolon = true;
        }
    }

    @Override
    public void visitForIn(@NotNull JsForIn x) {
        if (x.getBody() instanceof JsEmpty) {
            needsSemicolon = true;
        }
    }

    @Override
    public void visitIf(@NotNull JsIf x) {
        JsStatement thenStmt = x.getThenStatement();
        JsStatement elseStmt = x.getElseStatement();
        JsStatement toCheck = thenStmt;
        if (elseStmt != null) {
            toCheck = elseStmt;
        }
        if (toCheck instanceof JsEmpty) {
            needsSemicolon = true;
        }
        else {
            // Must recurse to determine last statement (possible if-else chain).
            accept(toCheck);
        }
    }

    @Override
    public void visitLabel(@NotNull JsLabel x) {
        if (x.getStatement() instanceof JsEmpty) {
            needsSemicolon = true;
        }
    }

    @Override
    public void visitWhile(@NotNull JsWhile x) {
        if (x.getBody() instanceof JsEmpty) {
            needsSemicolon = true;
        }
    }
}
