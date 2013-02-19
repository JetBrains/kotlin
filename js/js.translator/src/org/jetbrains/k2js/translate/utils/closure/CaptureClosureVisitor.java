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

package org.jetbrains.k2js.translate.utils.closure;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.k2js.translate.utils.BindingUtils;

class CaptureClosureVisitor extends JetTreeVisitor<ClosureContext> {
    @NotNull
    private final BindingContext bindingContext;
    @NotNull
    private final DeclarationDescriptor functionDescriptor;

    /*package*/ CaptureClosureVisitor(@NotNull DeclarationDescriptor descriptor, @NotNull BindingContext bindingContext) {
        this.bindingContext = bindingContext;
        functionDescriptor = descriptor;
    }

    @Override
    public Void visitJetElement(JetElement element, ClosureContext data) {
        if (element instanceof JetValueArgument) {
            JetExpression expression = ((JetValueArgument) element).getArgumentExpression();
            if (expression != null) {
                expression.accept(this, data);
            }
        }
        else if (!(element instanceof JetNamedFunction)) {
            return super.visitJetElement(element, data);
        }

        return null;
    }

    @Override
    public Void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression,
            @NotNull ClosureContext context) {
        if (expression.getNode().getElementType() == JetNodeTypes.OPERATION_REFERENCE) {
            return null;
        }

        DeclarationDescriptor descriptor = BindingUtils.getNullableDescriptorForReferenceExpression(bindingContext, expression);
        if (!(descriptor instanceof VariableDescriptor)) {
            if (descriptor instanceof SimpleFunctionDescriptor && !descriptor.getName().isSpecial()) {
                checkOuterClassDescriptor(descriptor, context);
            }
            return null;
        }

        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        if (captured(variableDescriptor, context)) {
            context.put(variableDescriptor);
        }

        return null;
    }

    private boolean captured(VariableDescriptor descriptor, ClosureContext context) {
        if (descriptor instanceof PropertyDescriptor) {
            checkOuterClassDescriptor(descriptor, context);
            return false;
        }

        // is not working, try test life - init is captured, but this method returns false
        //if (bindingContext.get(BindingContext.CAPTURED_IN_CLOSURE, descriptor) != Boolean.TRUE) {
        //    return false;
        //}
        
        if (isAncestor(functionDescriptor, descriptor)) {
            return false;
        }

        if (descriptor instanceof LocalVariableDescriptor && descriptor.isVar()) {
            // todo modification of outer local variable
            context.setHasLocalVariables();
            return true;
        }

        PsiElement variableDeclaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
        if (variableDeclaration == null) {
            // "it" doesn't have declaration
            return false;
        }

        return descriptor instanceof ValueParameterDescriptor ||
               (!descriptor.isVar() && variableDeclaration instanceof JetProperty) ||
               variableDeclaration.getNode().getElementType().equals(JetNodeTypes.LOOP_PARAMETER);
    }

    // differs from DescriptorUtils - fails if reach NamespaceDescriptor
    public static boolean isAncestor(@NotNull DeclarationDescriptor ancestor,
            @NotNull DeclarationDescriptor declarationDescriptor) {
        DeclarationDescriptor descriptor = declarationDescriptor.getContainingDeclaration();
        while (descriptor != null && !(descriptor instanceof NamespaceDescriptor)) {
            if (ancestor == descriptor) {
                return true;
            }
            descriptor = descriptor.getContainingDeclaration();
        }
        return false;
    }


    private static void checkOuterClassDescriptor(DeclarationDescriptor descriptor, ClosureContext context) {
        if (context.outerClassDescriptor == null) {
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                context.outerClassDescriptor = (ClassDescriptor) containingDeclaration;
            }
        }
    }
}
