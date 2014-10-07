/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.compiler.backend.js.ast;

/**
 * Taken from GWT project with modifications.
 * Original:
 *  repository: https://gwt.googlesource.com/gwt
 *  revision: e32bf0a95029165d9e6ab457c7ee7ca8b07b908c
 *  file: dev/core/src/com/google/gwt/dev/js/ast/JsModVisitor.java
 */

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Stack;

/**
 * A visitor for iterating through and modifying an AST.
 */
public class JsVisitorWithContextImpl extends JsVisitorWithContext {

    private final Stack<JsContext> statementContexts = new Stack<JsContext>();

    public class ListContext<T extends JsNode> implements JsContext {
        private List<T> collection;
        private int index;

        @Override
        public boolean canInsert() {
            return true;
        }

        @Override
        public boolean canRemove() {
            return true;
        }

        @Override
        public void insertAfter(JsNode node) {
            //noinspection unchecked
            collection.add(index + 1, (T) node);
        }

        @Override
        public void insertBefore(JsNode node) {
            //noinspection unchecked
            collection.add(index++, (T) node);
        }

        @Override
        public boolean isLvalue() {
            return false;
        }

        @Override
        public void removeMe() {
            collection.remove(index--);
        }

        @Override
        public void replaceMe(JsNode node) {
            checkReplacement(collection.get(index), node);
            //noinspection unchecked
            collection.set(index, (T) node);
        }
        
        @Nullable
        @Override
        public JsNode getCurrentNode() {
            if (index < collection.size()) {
                return collection.get(index);
            }

            return null;
        }

        protected void traverse(List<T> collection) {
            this.collection = collection;
            for (index = 0; index < collection.size(); ++index) {
                T node = collection.get(index);
                doTraverse(node, this);
            }
        }
    }

    private class LvalueContext extends NodeContext<JsExpression> {
        @Override
        public boolean isLvalue() {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private class NodeContext<T extends JsNode> implements JsContext {
        protected T node;

        @Override
        public boolean canInsert() {
            return false;
        }

        @Override
        public boolean canRemove() {
            return false;
        }

        @Override
        public void insertAfter(JsNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void insertBefore(JsNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLvalue() {
            return false;
        }

        @Override
        public void removeMe() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceMe(JsNode node) {
            checkReplacement(this.node, node);
            this.node = (T) node;
        }

        @Nullable
        @Override
        public JsNode getCurrentNode() {
            return node;
        }

        protected T traverse(T node) {
            this.node = node;
            doTraverse(node, this);
            return this.node;
        }
    }

    protected static void checkReplacement(@SuppressWarnings("UnusedParameters") JsNode origNode, JsNode newNode) {
        if (newNode == null) throw new RuntimeException("Cannot replace with null");
    }

    @Override
    protected <T extends JsNode> T doAccept(T node) {
        return new NodeContext<T>().traverse(node);
    }

    @Override
    protected JsExpression doAcceptLvalue(JsExpression expr) {
        return new LvalueContext().traverse(expr);
    }

    @Override
    protected <T extends JsStatement> JsStatement doAcceptStatement(T statement) {
        List<JsStatement> statements = new SmartList<JsStatement>(statement);
        doAcceptStatementList(statements);

        if (statements.size() == 1) {
            return statements.get(0);
        }

        return new JsBlock(statements);
    }

    @Override
    protected <T extends JsStatement> void doAcceptStatementList(List<T> statements) {
        ListContext<T> context = new ListContext<T>();
        statementContexts.push(context);
        context.traverse(statements);
        statementContexts.pop();
    }

    @Override
    protected <T extends JsNode> void doAcceptList(List<T> collection) {
        new ListContext<T>().traverse(collection);
    }

    @NotNull
    protected JsContext getLastStatementLevelContext() {
        return statementContexts.peek();
    }

    @Override
    protected <T extends JsNode> void doTraverse(T node, JsContext ctx) {
        node.traverse(this, ctx);
    }

}
