// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface JsNode {
    /**
     * Causes this object to have the visitor visit itself and its children.
     *
     * @param visitor the visitor that should traverse this node
     */
    void accept(JsVisitor visitor);

    void acceptChildren(JsVisitor visitor);

    /**
     * Return the source info associated with this object.
     */
    @Nullable JsLocationWithSource getSource();

    /**
     * Set the source info associated with this object.
     *
     * @param info
     */
    void setSource(@Nullable JsLocationWithSource info);

    @NotNull
    JsNode deepCopy();

    /**
     * Causes this object to have the visitor visit itself and its children.
     *
     * @param visitor the visitor that should traverse this node
     * @param ctx the context of an existing traversal
     */
    void traverse(JsVisitorWithContext visitor, JsContext ctx);

    List<JsComment> getCommentsBeforeNode();

    List<JsComment> getCommentsAfterNode();

    void setCommentsBeforeNode(List<JsComment> comment);

    void setCommentsAfterNode(List<JsComment> comment);
}
