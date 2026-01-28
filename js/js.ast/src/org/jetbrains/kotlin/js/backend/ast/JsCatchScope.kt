// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;

/**
 * A special scope used only for catch blocks. It only holds a single symbol:
 * the catch argument's name.
 */
public class JsCatchScope extends JsDeclarationScope {
    private final JsName name;

    public JsCatchScope(JsScope parent, @NotNull String ident) {
        super(parent, "Catch scope", true);
        name = new JsName(ident, false);
    }

    @Override
    @NotNull
    public JsName declareName(@NotNull String identifier) {
        // Declare into parent scope!
        return getParent().declareName(identifier);
    }

    @Override
    public boolean hasOwnName(@NotNull String name) {
        return findOwnName(name) != null;
    }

    @NotNull
    public JsCatchScope copy() {
        return new JsCatchScope(getParent(), name.getIdent());
    }

    @Override
    protected JsName findOwnName(@NotNull String ident) {
        return name.getIdent().equals(ident) ? name : null;
    }
}
