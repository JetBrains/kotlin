// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.google.dart.compiler.backend.js.ast.JsNumberLiteral.JsDoubleLiteral;
import static com.google.dart.compiler.backend.js.ast.JsNumberLiteral.JsIntLiteral;

/**
 * A JavaScript program.
 */
public final class JsProgram extends SourceInfoAwareJsNode {
    @NotNull
    private final JsEmpty emptyStatement;
    @NotNull final JsExpression emptyExpression;

    private JsProgramFragment[] fragments;

    private final TDoubleObjectHashMap<JsDoubleLiteral> doubleLiteralMap = new TDoubleObjectHashMap<JsDoubleLiteral>();
    private final TIntObjectHashMap<JsIntLiteral> intLiteralMap = new TIntObjectHashMap<JsIntLiteral>();

    private final JsRootScope rootScope;
    private final Map<String, JsStringLiteral> stringLiteralMap = new THashMap<String, JsStringLiteral>();
    private final JsObjectScope topScope;

    public JsProgram(String unitId) {
        rootScope = new JsRootScope(this);
        topScope = new JsObjectScope(rootScope, "Global", unitId);
        setFragmentCount(1);

        emptyStatement = new JsEmpty();
        emptyExpression = new JsEmptyExpression();
    }

    @NotNull
    public JsEmpty getEmptyStatement() {
        return emptyStatement;
    }

    @NotNull
    public JsExpression getEmptyExpression() {
        return emptyExpression;
    }

    public JsBlock getFragmentBlock(int fragment) {
        if (fragment < 0 || fragment >= fragments.length) {
            throw new IllegalArgumentException("Invalid fragment: " + fragment);
        }
        return fragments[fragment].getGlobalBlock();
    }

    public JsBlock getGlobalBlock() {
        return getFragmentBlock(0);
    }

    public JsNumberLiteral getNumberLiteral(double value) {
        JsDoubleLiteral literal = doubleLiteralMap.get(value);
        if (literal == null) {
            literal = new JsDoubleLiteral(value);
            doubleLiteralMap.put(value, literal);
        }

        return literal;
    }

    public JsNumberLiteral getNumberLiteral(int value) {
        JsIntLiteral literal = intLiteralMap.get(value);
        if (literal == null) {
            literal = new JsIntLiteral(value);
            intLiteralMap.put(value, literal);
        }

        return literal;
    }

    /**
     * Gets the quasi-mythical root scope. This is not the same as the top scope;
     * all unresolvable identifiers wind up here, because they are considered
     * external to the program.
     */
    public JsRootScope getRootScope() {
        return rootScope;
    }

    /**
     * Gets the top level scope. This is the scope of all the statements in the
     * main program.
     */
    public JsObjectScope getScope() {
        return topScope;
    }

    /**
     * Creates or retrieves a JsStringLiteral from an interned object pool.
     */
    @NotNull
    public JsStringLiteral getStringLiteral(String value) {
        JsStringLiteral literal = stringLiteralMap.get(value);
        if (literal == null) {
            literal = new JsStringLiteral(value);
            stringLiteralMap.put(value, literal);
        }
        return literal;
    }

    public void setFragmentCount(int fragments) {
        this.fragments = new JsProgramFragment[fragments];
        for (int i = 0; i < fragments; i++) {
            this.fragments[i] = new JsProgramFragment();
        }
    }

    @Override
    public void accept(JsVisitor v) {
        v.visitProgram(this);
    }

    @Override
    public void acceptChildren(JsVisitor visitor) {
        for (JsProgramFragment fragment : fragments) {
            visitor.accept(fragment);
        }
    }

    @Override
    public void traverse(JsVisitorWithContext v, JsContext ctx) {
        if (v.visit(this, ctx)) {
            for (JsProgramFragment fragment : fragments) {
                v.accept(fragment);
            }
        }
        v.endVisit(this, ctx);
    }

    @NotNull
    @Override
    public JsProgram deepCopy() {
        throw new UnsupportedOperationException();
    }
}
