/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.common.collect.Maps;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.Map;

import static org.jetbrains.k2js.translate.utils.PsiUtils.getBaseExpression;

//TODO: refactor
public final class FindPreviousVisitor extends JetTreeVisitor<DangerousData> {

    @NotNull
    private final Map<JetElement, Void> hasDangerous = Maps.newHashMap();

    public FindPreviousVisitor(@NotNull DangerousData data) {
        JetElement node = data.getDangerousNode();
        PsiElement last = data.getRootNode().getParent();
        while (node != last) {
            hasDangerous.put(node, null);
            PsiElement parent = node.getParent();
            assert parent instanceof JetElement;
            node = (JetElement)parent;
        }
    }

    @Override
    public Void visitJetElement(JetElement element, DangerousData data) {
        if (data.getDangerousNode() == element) {
            return null;
        }
        if (!hasDangerous(element)) {
            addElement(element, data);
        }
        else {
            acceptChildrenThatAreBeforeTheDangerousNode(element, data);
        }
        return null;
    }

    //TODO: return value not used, wtf?
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
                if (hasDangerous(element)) {
                    break;
                }
            }
            current = current.getNextSibling();
        }
    }

    @Override
    public Void visitPrefixExpression(@NotNull JetPrefixExpression expression, @NotNull DangerousData data) {
        if (data.getDangerousNode() == expression) {
            return null;
        }
        if (!hasDangerous(expression)) {
            addElement(expression, data);
            return null;
        }
        else {
            if (hasDangerous(getBaseExpression(expression))) {
                return null;
            }
            else {
                //TODO:
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public Void visitCallExpression(@NotNull JetCallExpression expression, @NotNull DangerousData data) {
        if (data.getDangerousNode() == expression) {
            return null;
        }
        if (!hasDangerous(expression)) {
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
            if (hasDangerous(argumentExpression)) {
                break;
            }
        }
    }

    private boolean hasDangerous(@NotNull JetElement element) {
        return hasDangerous.containsKey(element);
    }
}
