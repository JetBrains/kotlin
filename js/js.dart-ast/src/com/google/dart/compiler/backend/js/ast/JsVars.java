// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.Symbol;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A JavaScript <code>var</code> statement.
 */
public class JsVars extends SourceInfoAwareJsNode implements JsStatement, Iterable<JsVars.JsVar> {
    private final List<JsVar> vars;

    private final boolean multiline;

    public JsVars() {
        this(new SmartList<JsVar>(), false);
    }

    public JsVars(boolean multiline) {
        this(new SmartList<JsVar>(), multiline);
    }

    public JsVars(List<JsVar> vars, boolean multiline) {
        this.vars = vars;
        this.multiline = multiline;
    }

    public JsVars(JsVar var) {
        this(Collections.singletonList(var), false);
    }

    public JsVars(JsVar... vars) {
        this(Arrays.asList(vars), false);
    }

    public boolean isMultiline() {
        return multiline;
    }

    /**
     * A var declared using the JavaScript <code>var</code> statement.
     */
    public static class JsVar extends SourceInfoAwareJsNode implements HasName {
        private final JsName name;
        private JsExpression initExpression;

        public JsVar(JsName name) {
            this.name = name;
        }

        public JsVar(JsName name, @Nullable JsExpression initExpression) {
            this.name = name;
            this.initExpression = initExpression;
        }

        public JsExpression getInitExpression() {
            return initExpression;
        }

        @Override
        public JsName getName() {
            return name;
        }

        @Override
        public Symbol getSymbol() {
            return name;
        }

        public void setInitExpression(JsExpression initExpression) {
            this.initExpression = initExpression;
        }

        @Override
        public void accept(JsVisitor v) {
            v.visit(this);
        }

        @Override
        public void acceptChildren(JsVisitor visitor) {
            if (initExpression != null) {
                visitor.accept(initExpression);
            }
        }
    }

    public void add(JsVar var) {
        vars.add(var);
    }

    public void addAll(Collection<? extends JsVars.JsVar> vars) {
        this.vars.addAll(vars);
    }

    public void addAll(JsVars otherVars) {
        this.vars.addAll(otherVars.vars);
    }

    public void addIfHasInitializer(JsVar var) {
        if (var.getInitExpression() != null) {
            add(var);
        }
    }

    public boolean isEmpty() {
        return vars.isEmpty();
    }

    @Override
    public Iterator<JsVar> iterator() {
        return vars.iterator();
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitVars(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        visitor.acceptWithInsertRemove(vars);
    }
}
