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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author Pavel Talanov
 */
public final class FindPreviousVisitor extends JetTreeVisitor<DangerousData> {

    @Override
    public Void visitJetElement(JetElement element, DangerousData data) {
        if (data.getDangerousNode() == element) {
            return null;
        }
        if (!hasDangerous(element, data)) {
            addElement(element, data);
        }
        else {
            acceptChildrenThatAreBeforeTheDangerousNode(element, data);
        }
        return null;
    }

    private static boolean addElement(@NotNull JetElement element, @NotNull DangerousData data) {
        if (element instanceof JetExpression) {
            data.getNodesToBeGeneratedBefore().add((JetExpression)element);
            return true;
        }
        return false;
    }

    private void acceptChildrenThatAreBeforeTheDangerousNode(@NotNull JetElement element, @NotNull DangerousData data) {
        PsiElement current = element.getFirstChild();
        while (current != null) {
            if (current instanceof JetElement) {
                ((JetElement)current).accept(this, data);
                if (hasDangerous(element, data)) {
                    break;
                }
            }
            current = current.getNextSibling();
        }
    }

    @Override
    public Void visitCallExpression(@NotNull JetCallExpression expression, @NotNull DangerousData data) {
        if (data.getDangerousNode() == expression) {
            return null;
        }
        if (!hasDangerous(expression, data)) {
            data.getNodesToBeGeneratedBefore().add(expression);
        }
        else {
            acceptArgumentsThatAreBeforeDangerousNode(expression, data);
        }
        return null;
    }

    private void acceptArgumentsThatAreBeforeDangerousNode(@NotNull JetCallExpression expression, @NotNull DangerousData data) {
        for (ValueArgument argument : expression.getValueArguments()) {
            JetExpression argumentExpression = argument.getArgumentExpression();
            assert argumentExpression != null;
            argumentExpression.accept(this, data);
            if (hasDangerous(argumentExpression, data)) {
                break;
            }
        }
    }

    private static boolean hasDangerous(@NotNull JetElement element, @NotNull DangerousData data) {
        HasDangerousVisitor visitor = new HasDangerousVisitor();
        element.accept(visitor, data);
        return visitor.hasDangerous;
    }

    private static final class HasDangerousVisitor extends JetTreeVisitor<DangerousData> {

        private boolean hasDangerous = false;

        @Override
        public Void visitJetElement(JetElement element, DangerousData data) {
            if (element == data.getDangerousNode()) {
                hasDangerous = true;
                return null;
            }
            element.acceptChildren(this, data);
            return null;
        }
    }
}
