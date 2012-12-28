/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.translate.utils.dangerous;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

/**
 * This module uses a methaphor for naming.
 * <p/>
 * Dangerous are the nodes that can be expressions in Kotlin but can't be expressions in JavaScript.
 * These are: when, if, inlined functions.
 * The issue with them is that we have to translate them to a list of statements. And also all the expressions which must be computed before
 * the dangerous expressions.
 * RootNode is a node which contains such an expression. For example, it may be a statement expression belongs to.
 */
public class DangerousData {
    @NotNull
    private final List<JetExpression> nodesToBeGeneratedBefore = Lists.newArrayList();


    @NotNull
    public static DangerousData collect(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        if (cantContainDangerousElements(expression)) {
            return emptyData();
        }
        return doCollectData(expression, context);
    }

    private static boolean cantContainDangerousElements(@NotNull JetElement element) {
        return element instanceof JetBlockExpression;
    }

    @NotNull
    private static DangerousData doCollectData(@NotNull JetExpression expression,
                                               @NotNull TranslationContext context) {
        DangerousData data = new DangerousData();
        FindDangerousVisitor visitor = new FindDangerousVisitor(context);
        expression.accept(visitor, data);
        if (!data.exists()) {
            return emptyData();
        }
        data.setRootNode(expression);
        FindPreviousVisitor findPreviousVisitor = new FindPreviousVisitor(data);
        expression.accept(findPreviousVisitor, data);
        return data;
    }

    private static final DangerousData EMPTY = new DangerousData() {
        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public boolean shouldBeTranslated() {
            return false;
        }
    };

    @NotNull
    public static DangerousData emptyData() {
        return EMPTY;
    }

    @Nullable
    private JetExpression dangerousNode = null;

    @Nullable
    private JetExpression rootNode = null;

    public void setDangerousNode(@NotNull JetExpression dangerousNode) {
        assert this.dangerousNode == null : "Should be assigned only once";
        this.dangerousNode = dangerousNode;
    }

    @NotNull
    public List<JetExpression> getNodesToBeGeneratedBefore() {
        return nodesToBeGeneratedBefore;
    }

    public boolean exists() {
        return dangerousNode != null;
    }

    public boolean shouldBeTranslated() {
        return exists() && !nodesToBeGeneratedBefore.isEmpty();
    }

    @NotNull
    public JetExpression getDangerousNode() {
        assert dangerousNode != null;
        return dangerousNode;
    }

    @NotNull
    public JetExpression getRootNode() {
        assert rootNode != null;
        return rootNode;
    }

    @SuppressWarnings("NullableProblems")
    public void setRootNode(@NotNull JetExpression rootNode) {
        this.rootNode = rootNode;
    }
}
