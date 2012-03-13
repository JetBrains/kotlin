/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.utils.closure;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetTreeVisitor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;

/**
 * @author Pavel Talanov
 */
public class CaptureClosureVisitor extends JetTreeVisitor<ClosureContext> {

    @NotNull
    private final BindingContext bindingContext;
    @NotNull
    private final JetElement functionElement;

    /*package*/ CaptureClosureVisitor(@NotNull JetElement functionElement, @NotNull BindingContext bindingContext) {
        this.bindingContext = bindingContext;
        this.functionElement = functionElement;

    }

    @Override
    public Void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression,
                                          @NotNull ClosureContext context) {
        expression.acceptChildren(this, context);
        DeclarationDescriptor descriptor = BindingUtils.getNullableDescriptorForReferenceExpression(bindingContext, expression);
        if (!(descriptor instanceof VariableDescriptor)) {
            return null;
        }
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        PsiElement variableDeclaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if (variableDeclaration == null) {
            return null;
        }
        if (PsiTreeUtil.isAncestor(functionElement, variableDeclaration, false)) {
            return null;
        }
        boolean isLoopParameter = variableDeclaration.getNode().getElementType().equals(JetNodeTypes.LOOP_PARAMETER);
        if (isLoopParameter) {
            context.put(variableDescriptor);
        }
        return null;
    }

}
