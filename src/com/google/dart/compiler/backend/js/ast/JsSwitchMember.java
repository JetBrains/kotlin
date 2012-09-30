// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.intellij.util.SmartList;

import java.util.List;

/**
 * A member/case in a JavaScript switch object.
 */
public abstract class JsSwitchMember extends JsNodeImpl {
    protected final List<JsStatement> statements = new SmartList<JsStatement>();

    protected JsSwitchMember() {
        super();
    }

    public List<JsStatement> getStatements() {
        return statements;
    }
}
