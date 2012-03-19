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

package org.jetbrains.k2js.translate.utils;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.BindingContext.INDEXED_LVALUE_GET;
import static org.jetbrains.jet.lang.resolve.BindingContext.INDEXED_LVALUE_SET;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.*;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This class contains some code related to BindingContext use. Intention is not to pollute other classes.
 *         Every call to BindingContext.get() is supposed to be wrapped by this utility class.
 */
public final class BindingUtils {

    private BindingUtils() {
    }

    @NotNull
    static private <E extends PsiElement, D extends DeclarationDescriptor>
    D getDescriptorForExpression(@NotNull BindingContext context, @NotNull E expression, Class<D> descriptorClass) {
        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression);
        assert descriptor != null;
        assert descriptorClass.isInstance(descriptor)
            : expression.toString() + " expected to have of type" + descriptorClass.toString();
        //noinspection unchecked
        return (D)descriptor;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptor(@NotNull BindingContext context,
                                                     @NotNull JetClassOrObject declaration) {
        return getDescriptorForExpression(context, declaration, ClassDescriptor.class);
    }

    @NotNull
    public static NamespaceDescriptor getNamespaceDescriptor(@NotNull BindingContext context,
                                                             @NotNull JetFile declaration) {
        NamespaceDescriptor namespaceDescriptor =
            context.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, JetPsiUtil.getFQName(declaration));
        assert namespaceDescriptor != null : "File should have a namespace descriptor.";
        return namespaceDescriptor;
    }

    @NotNull
    public static FunctionDescriptor getFunctionDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetDeclarationWithBody declaration) {
        return getDescriptorForExpression(context, declaration, FunctionDescriptor.class);
    }

    @NotNull
    public static PropertyDescriptor getPropertyDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetProperty declaration) {
        return getDescriptorForExpression(context, declaration, PropertyDescriptor.class);
    }

    @NotNull
    public static JetClass getClassForDescriptor(@NotNull BindingContext context,
                                                 @NotNull ClassDescriptor descriptor) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        assert result instanceof JetClass : "ClassDescriptor should have declaration of type JetClass";
        return (JetClass)result;
    }

    @NotNull
    public static List<JetDeclaration> getDeclarationsForNamespace(@NotNull BindingContext bindingContext,
                                                                   @NotNull NamespaceDescriptor namespace) {
        List<JetDeclaration> declarations = new ArrayList<JetDeclaration>();
        for (DeclarationDescriptor descriptor : getContainedDescriptorsWhichAreNotPredefined(namespace)) {
            if (descriptor instanceof NamespaceDescriptor) {
                continue;
            }
            JetDeclaration declaration = getDeclarationForDescriptor(bindingContext, descriptor);
            if (declaration != null) {
                declarations.add(declaration);
            }
        }
        return declarations;
    }

    @Nullable
    private static JetDeclaration getDeclarationForDescriptor(@NotNull BindingContext context,
                                                              @NotNull DeclarationDescriptor descriptor) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if (result == null) {
            //TODO: never get there
            return null;
        }
        assert result instanceof JetDeclaration : "Descriptor should correspond to an element.";
        return (JetDeclaration)result;
    }

    @NotNull
    private static JetParameter getParameterForDescriptor(@NotNull BindingContext context,
                                                          @NotNull ValueParameterDescriptor descriptor) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        assert result instanceof JetParameter : "ValueParameterDescriptor should have corresponding JetParameter.";
        return (JetParameter)result;
    }

    public static boolean hasAncestorClass(@NotNull BindingContext context, @NotNull JetClassOrObject classDeclaration) {
        ClassDescriptor classDescriptor = getClassDescriptor(context, classDeclaration);
        List<ClassDescriptor> superclassDescriptors = getSuperclassDescriptors(classDescriptor);
        return (DescriptorUtils.findAncestorClass(superclassDescriptors) != null);
    }

    public static boolean isStatement(@NotNull BindingContext context, @NotNull JetExpression expression) {
        Boolean isStatement = context.get(BindingContext.STATEMENT, expression);
        assert isStatement != null : "Invalid behaviour of get(BindingContext.STATEMENT)";
        return isStatement;
        // return IsStatement.isStatement(expression);
    }

    @NotNull
    public static JetType getTypeByReference(@NotNull BindingContext context,
                                             @NotNull JetTypeReference typeReference) {
        JetType result = context.get(BindingContext.TYPE, typeReference);
        assert result != null : "TypeReference should reference a type";
        return result;
    }

    @NotNull
    public static ClassDescriptor getClassDescriptorForTypeReference(@NotNull BindingContext context,
                                                                     @NotNull JetTypeReference typeReference) {
        return getClassDescriptorForType(getTypeByReference(context, typeReference));
    }

    @Nullable
    public static PropertyDescriptor getPropertyDescriptorForConstructorParameter(@NotNull BindingContext context,
                                                                                  @NotNull JetParameter parameter) {
        return context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
    }

    @Nullable
    public static JetProperty getPropertyForDescriptor(@NotNull BindingContext context,
                                                       @NotNull PropertyDescriptor property) {
        PsiElement result = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, property);
        if (!(result instanceof JetProperty)) {
            return null;
        }
        return (JetProperty)result;
    }

    @NotNull
    public static DeclarationDescriptor getDescriptorForReferenceExpression(@NotNull BindingContext context,
                                                                            @NotNull JetReferenceExpression reference) {
        DeclarationDescriptor referencedDescriptor = getNullableDescriptorForReferenceExpression(context, reference);
        assert referencedDescriptor != null : "Reference expression must reference a descriptor.";
        return referencedDescriptor;
    }

    @Nullable
    public static DeclarationDescriptor getNullableDescriptorForReferenceExpression(@NotNull BindingContext context,
                                                                                    @NotNull JetReferenceExpression reference) {
        DeclarationDescriptor referencedDescriptor = context.get(BindingContext.REFERENCE_TARGET, reference);
        if (isVariableAsFunction(referencedDescriptor)) {
            assert referencedDescriptor != null;
            return getVariableDescriptorForVariableAsFunction((VariableAsFunctionDescriptor)referencedDescriptor);
        }
        return referencedDescriptor;
    }

    public static boolean isNotAny(@NotNull DeclarationDescriptor superClassDescriptor) {
        return !superClassDescriptor.equals(JetStandardClasses.getAny());
    }

    @NotNull
    public static ResolvedCall<?> getResolvedCall(@NotNull BindingContext context,
                                                  @NotNull JetExpression expression) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = context.get(BindingContext.RESOLVED_CALL, expression);
        assert resolvedCall != null : "Must resolve to a call.";
        return resolvedCall;
    }

    @NotNull
    public static ResolvedCall<?> getResolvedCallForCallExpression(@NotNull BindingContext context,
                                                                   @NotNull JetCallExpression expression) {
        JetExpression calleeExpression = PsiUtils.getCallee(expression);
        return getResolvedCall(context, calleeExpression);
    }

    public static boolean isVariableReassignment(@NotNull BindingContext context, @NotNull JetExpression expression) {
        Boolean result = context.get(BindingContext.VARIABLE_REASSIGNMENT, expression);
        assert result != null;
        return result;
    }


    @Nullable
    public static FunctionDescriptor getFunctionDescriptorForOperationExpression(@NotNull BindingContext context,
                                                                                 @NotNull JetOperationExpression expression) {
        DeclarationDescriptor descriptorForReferenceExpression = getNullableDescriptorForReferenceExpression
            (context, expression.getOperationReference());

        if (descriptorForReferenceExpression == null) return null;

        assert descriptorForReferenceExpression instanceof FunctionDescriptor
            : "Operation should resolve to function descriptor.";
        return (FunctionDescriptor)descriptorForReferenceExpression;
    }

    @NotNull
    public static DeclarationDescriptor getDescriptorForElement(@NotNull BindingContext context,
                                                                @NotNull PsiElement element) {
        DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        assert descriptor != null : element + " doesn't have a descriptor.";
        return descriptor;
    }

    @Nullable
    public static Object getCompileTimeValue(@NotNull BindingContext context, @NotNull JetExpression expression) {
        CompileTimeConstant<?> compileTimeValue = context.get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (compileTimeValue != null) {
            return compileTimeValue.getValue();
        }
        return null;
    }

    @NotNull
    public static JetExpression getDefaultArgument(@NotNull BindingContext context,
                                                   @NotNull ValueParameterDescriptor parameterDescriptor) {
        assert parameterDescriptor.hasDefaultValue() : "Unsupplied parameter must have default value.";
        JetParameter psiParameter = getParameterForDescriptor(context, parameterDescriptor);
        JetExpression defaultValue = psiParameter.getDefaultValue();
        assert defaultValue != null : "No default value found in PSI.";
        return defaultValue;
    }

    @NotNull
    public static FunctionDescriptor getIteratorFunction(@NotNull BindingContext context,
                                                         @NotNull JetExpression rangeExpression) {
        FunctionDescriptor functionDescriptor = context.get(BindingContext.LOOP_RANGE_ITERATOR, rangeExpression);
        assert functionDescriptor != null : "Range expression must have a descriptor for iterator function.";
        return functionDescriptor;
    }

    @NotNull
    public static FunctionDescriptor getNextFunction(@NotNull BindingContext context,
                                                     @NotNull JetExpression rangeExpression) {
        FunctionDescriptor functionDescriptor = context.get(BindingContext.LOOP_RANGE_NEXT, rangeExpression);
        assert functionDescriptor != null : "Range expression must have a descriptor for next function.";
        return functionDescriptor;
    }

    @NotNull
    public static CallableDescriptor getHasNextCallable(@NotNull BindingContext context,
                                                        @NotNull JetExpression rangeExpression) {
        CallableDescriptor hasNextDescriptor = context.get(BindingContext.LOOP_RANGE_HAS_NEXT, rangeExpression);
        assert hasNextDescriptor != null : "Range expression must have a descriptor for hasNext function or property.";
        return hasNextDescriptor;
    }

    @NotNull
    public static PropertyDescriptor getPropertyDescriptorForObjectDeclaration(@NotNull BindingContext context,
                                                                               @NotNull JetObjectDeclarationName name) {
        PropertyDescriptor propertyDescriptor = context.get(BindingContext.OBJECT_DECLARATION, name);
        assert propertyDescriptor != null;
        return propertyDescriptor;
    }

    @NotNull
    public static Set<NamespaceDescriptor> getAllNonNativeNamespaceDescriptors(@NotNull BindingContext context,
                                                                               @NotNull List<JetFile> files) {
        Set<NamespaceDescriptor> descriptorSet = Sets.newHashSet();
        for (JetFile file : files) {
            //TODO: can't be
            NamespaceDescriptor namespaceDescriptor = getNamespaceDescriptor(context, file);
            if (!AnnotationsUtils.isPredefinedObject(namespaceDescriptor)) {
                descriptorSet.addAll(getNamespaceDescriptorHierarchy(namespaceDescriptor));
            }
        }
        return descriptorSet;
    }

    @NotNull
    public static JetType getTypeForExpression(@NotNull BindingContext context,
                                               @NotNull JetExpression expression) {
        JetType type = context.get(BindingContext.EXPRESSION_TYPE, expression);
        assert type != null;
        return type;
    }

    @NotNull
    public static ResolvedCall<FunctionDescriptor> getResolvedCallForArrayAccess(@NotNull BindingContext context,
                                                                                 @NotNull JetArrayAccessExpression arrayAccessExpression,
                                                                                 boolean isGet) {
        ResolvedCall<FunctionDescriptor> resolvedCall = context.get(isGet
                                                                    ? INDEXED_LVALUE_GET
                                                                    : INDEXED_LVALUE_SET, arrayAccessExpression);
        assert resolvedCall != null;
        return resolvedCall;
    }

    public static ConstructorDescriptor getConstructor(@NotNull BindingContext bindingContext,
                                                       @NotNull JetClassOrObject declaration) {
        ConstructorDescriptor primaryConstructor =
            getClassDescriptor(bindingContext, declaration).getUnsubstitutedPrimaryConstructor();
        assert primaryConstructor != null : "Traits do not have initialize methods.";
        return primaryConstructor;
    }
}
