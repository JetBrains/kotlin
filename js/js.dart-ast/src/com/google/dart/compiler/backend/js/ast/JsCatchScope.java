// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.NotNull;

/**
 * A special scope used only for catch blocks. It only holds a single symbol:
 * the catch argument's name.
 */
public class JsCatchScope extends JsScope {
    private final JsName name;

    public JsCatchScope(JsScope parent, String ident) {
        super(parent, "Catch scope");
        name = new JsName(this, ident);
    }

    @Override
    @NotNull
    public JsName declareName(String identifier) {
        // Declare into parent scope!
        return getParent().declareName(identifier);
    }

    @Override
    public boolean hasOwnName(@NotNull String name) {
        return findOwnName(name) != null;
    }

    @Override
    protected JsName findOwnName(String ident) {
        return name.getIdent().equals(ident) ? name : null;
    }
}
