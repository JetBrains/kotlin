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

package org.jetbrains.kotlin.js.backend.ast;

/*
 * Taken from GWT project with modifications.
 * Original:
 *  repository: https://gwt.googlesource.com/gwt
 *  revision: e32bf0a95029165d9e6ab457c7ee7ca8b07b908c
 *  file: dev/core/src/com/google/gwt/dev/js/ast/JsModVisitor.java
 */

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A visitor for iterating through and modifying an AST.
 */
public class JsVisitorWithContextImpl extends JsVisitorWithContext {

    protected final Stack<JsContext<JsStatement>> statementContexts = new Stack<>();

    public class ListContext<T extends JsNode> extends JsContext<T> {
        private List<T> nodes;
        private int index;

        // Those are reset in every iteration of traverse()
        private final List<T> previous = new SmartList<>();
        private final List<T> next = new SmartList<>();
        private boolean removed = false;

        @Override
        public <R extends T> void addPrevious(R node) {
            previous.add(node);
        }

        @Override
        public <R extends T> void addNext(R node) {
            next.add(node);
        }

        @Override
        public void removeMe() {
            removed = true;
        }

        @Override
        public <R extends T> void replaceMe(R node) {
            checkReplacement(nodes.get(index), node);
            nodes.set(index, node);
            removed = false;
        }
        
        @Nullable
        @Override
        public T getCurrentNode() {
            if (!removed && index < nodes.size()) {
                return nodes.get(index);
            }

            return null;
        }

        public void traverse(List<T> nodes) {
            assert previous.isEmpty(): "addPrevious() was called before traverse()";
            assert next.isEmpty(): "addNext() was called before traverse()";
            this.nodes = nodes;

            for (index = 0; index < nodes.size(); index++) {
                removed = false;
                previous.clear();
                next.clear();
                doTraverse(getCurrentNode(), this);

                if (!previous.isEmpty()) {
                    nodes.addAll(index, previous);
                    index += previous.size();
                }

                if (removed) {
                    nodes.remove(index);
                    index--;
                }

                if (!next.isEmpty()) {
                    nodes.addAll(index + 1, next);
                    index += next.size();
                }
            }

            previous.clear();
            next.clear();
        }
    }

    private class LvalueContext extends NodeContext<JsExpression> {
    }

    private class NodeContext<T extends JsNode> extends JsContext<T> {
        protected T node;

        @Override
        public void removeMe() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R extends T> void replaceMe(R node) {
            checkReplacement(this.node, node);
            this.node = node;
        }

        @Nullable
        @Override
        public T getCurrentNode() {
            return node;
        }

        protected T traverse(T node) {
            this.node = node;
            doTraverse(node, this);
            return this.node;
        }
    }

    private static void checkReplacement(@SuppressWarnings("UnusedParameters") JsNode origNode, JsNode newNode) {
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
        List<JsStatement> statements = new SmartList<>(statement);
        doAcceptStatementList(statements);

        if (statements.size() == 1) {
            return statements.get(0);
        }

        return new JsBlock(statements);
    }

    @Override
    protected void doAcceptStatementList(List<JsStatement> statements) {
        ListContext<JsStatement> context = new ListContext<>();
        statementContexts.push(context);
        context.traverse(statements);
        statementContexts.pop();
    }

    @Override
    protected <T extends JsNode> void doAcceptList(List<T> collection) {
        new ListContext<T>().traverse(collection);
    }

    @NotNull
    protected JsContext<JsStatement> getLastStatementLevelContext() {
        return statementContexts.peek();
    }

    @Override
    protected <T extends JsNode> void doTraverse(T node, JsContext ctx) {
        node.traverse(this, ctx);
    }

}
