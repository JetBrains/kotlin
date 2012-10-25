// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.HasSourceInfo;

public interface JsNode extends HasSourceInfo {
    NodeKind getKind();

    /**
     * Causes this object to have the visitor visit itself and its children.
     *
     * @param visitor the visitor that should traverse this node
     * @param context the context of an existing traversal
     */
    void accept(JsVisitor visitor, JsContext context);

    void acceptChildren(JsVisitor visitor, JsContext context);
}
