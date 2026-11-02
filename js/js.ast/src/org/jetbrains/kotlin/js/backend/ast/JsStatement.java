// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package org.jetbrains.kotlin.js.backend.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.metadata.HasMetadata;

public interface JsStatement extends JsNode, HasMetadata {
    @NotNull
    @Override
    JsStatement deepCopy();
}
