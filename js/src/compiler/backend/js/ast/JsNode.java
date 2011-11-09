// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package compiler.backend.js.ast;

import compiler.backend.js.JsSourceGenerationVisitor;
import compiler.backend.js.JsToStringGenerationVisitor;
import compiler.common.AbstractNode;
import compiler.common.SourceInfo;
import compiler.util.DefaultTextOutput;

/**
 * Base class for all JS AST elements.
 */
public abstract class JsNode extends AbstractNode implements JsVisitable {

  protected JsNode() {
  }

  // Causes source generation to delegate to the one visitor
  public final String toSource() {
    DefaultTextOutput out = new DefaultTextOutput(false);
    JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out);
    v.accept(this);
    return out.toString();
  }

  // Causes source generation to delegate to the one visitor
  @Override
  public final String toString() {
    DefaultTextOutput out = new DefaultTextOutput(false);
    JsToStringGenerationVisitor v = new JsToStringGenerationVisitor(out);
    v.accept(this);
    return out.toString();
  }

  @Override
  public SourceInfo getSourceInfo() {
    return this;
  }

  public JsNode setSourceRef(SourceInfo info) {
    if (info != null) {
      this.setSourceInfo(info);
    }
    return this;
  }

  public abstract NodeKind getKind();
}
