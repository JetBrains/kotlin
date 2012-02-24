// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaScript function expression.
 */
public final class JsFunction extends JsLiteral implements HasName {

    private static void trace(String title, String code) {
        System.out.println("---------------------------");
        System.out.println(title + ":");
        System.out.println("---------------------------");
        System.out.println(code);
    }

    protected JsBlock body;
    protected final List<JsParameter> params = new ArrayList<JsParameter>();
    protected final JsScope scope;
    private boolean artificiallyRescued;
    private boolean executeOnce;
    private boolean fromDart;
    private JsFunction impliedExecute;
    private JsName name;
    private boolean trace = false;
    private boolean traceFirst = true;
    private boolean hoisted = false;
    private boolean constructor = false;

    /**
     * Creates an anonymous function.
     */
    public JsFunction(JsScope parent) {
        this(parent, null, false);
    }

    /**
     * Creates a function that is not derived from Dart source.
     */
    public JsFunction(JsScope parent, JsName name) {
        this(parent, name, false);
    }

    /**
     * Creates a named function, possibly derived from Dart source.
     */
    public JsFunction(JsScope parent, JsName name, boolean fromDart) {
        assert (parent != null);
        this.fromDart = fromDart;
        setName(name);
        String scopeName = (name == null) ? "<anonymous>" : name.getIdent();
        scopeName = "function " + scopeName;
        this.scope = new JsScope(parent, scopeName);
    }

    public JsBlock getBody() {
        return body;
    }

    /**
     * If true, this indicates that only the first invocation of the function will
     * have any effects. Subsequent invocations may be considered to be no-op
     * calls whose return value is ignored.
     */
    public boolean getExecuteOnce() {
        return executeOnce;
    }

    public JsFunction getImpliedExecute() {
        return impliedExecute;
    }

    @Override
    public JsName getName() {
        return name;
    }

    public void setParameters(List<JsParameter> params) {
        this.params.clear();
        this.params.addAll(params);
    }

    public List<JsParameter> getParameters() {
        return params;
    }

    public JsScope getScope() {
        return scope;
    }

    @Override
    public boolean hasSideEffects() {
        // If there's a name, the name is assigned to.
        return name != null;
    }

    public boolean isArtificiallyRescued() {
        return artificiallyRescued;
    }

    @Override
    public boolean isBooleanFalse() {
        return false;
    }

    @Override
    public boolean isBooleanTrue() {
        return true;
    }

    @Override
    public boolean isDefinitelyNotNull() {
        return true;
    }

    @Override
    public boolean isDefinitelyNull() {
        return false;
    }

    public boolean isFromDart() {
        return fromDart;
    }

    public void setArtificiallyRescued(boolean rescued) {
        this.artificiallyRescued = rescued;
    }

    public void setBody(JsBlock body) {
        this.body = body;
    }

    public void setExecuteOnce(boolean executeOnce) {
        this.executeOnce = executeOnce;
    }

    public void setFromDart(boolean fromDart) {
        this.fromDart = fromDart;
    }

    public void setImpliedExecute(JsFunction impliedExecute) {
        this.impliedExecute = impliedExecute;
    }

    public void setName(JsName name) {
        this.name = name;
        if (name != null) {
            if (isFromDart()) {
                name.setStaticRef(this);
            }
        }
    }

    public void setTrace() {
        this.trace = true;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
        String before = null;
        if (trace && v instanceof JsModVisitor) {
            before = this.toSource();
            if (traceFirst) {
                traceFirst = false;
                trace("SCRIPT INITIAL", before);
            }
        }
        if (v.visit(this, ctx)) {
            v.acceptWithInsertRemove(params);
            body = v.accept(body);
        }
        v.endVisit(this, ctx);
        if (trace && v instanceof JsModVisitor) {
            String after = this.toSource();
            if (!after.equals(before)) {
                String title = v.getClass().getSimpleName();
                trace(title, after);
            }
        }
    }

    public void setHoisted() {
        hoisted = true;
    }

    /**
     * Whether the function has been hoisted
     */
    public boolean isHoisted() {
        return hoisted;
    }

    /**
     * Rebase the function to a new scope.
     *
     * @param newScopeParent The scope to add the function to.
     */
    public void rebaseScope(JsScope newScopeParent) {
        this.scope.rebase(newScopeParent);
    }

    @Override
    public JsFunction setSourceRef(SourceInfo info) {
        super.setSourceRef(info);
        return this;
    }

    public boolean isConstructor() {
        return this.constructor;
    }

    public boolean setIsConstructor(boolean constructor) {
        return this.constructor = constructor;
    }

    @Override
    public NodeKind getKind() {
        return NodeKind.FUNCTION;
    }

    // Pavel Talanov
    // dummy parameter to distinguish from other constructors
    private JsFunction(Void dummy, JsScope functionScope) {
        this.fromDart = false;
        this.scope = functionScope;
    }

    public static JsFunction getAnonymousFunctionWithScope(JsScope scope) {
        return new JsFunction(null, scope);
    }
}
