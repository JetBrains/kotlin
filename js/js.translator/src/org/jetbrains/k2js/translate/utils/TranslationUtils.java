/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.context.TemporaryConstVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.*;

import static com.google.dart.compiler.backend.js.ast.JsBinaryOperator.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;
import static org.jetbrains.k2js.translate.context.Namer.getKotlinBackingFieldName;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.createDataDescriptor;

public final class TranslationUtils {
    public static final Comparator<FunctionDescriptor> OVERLOADED_FUNCTION_COMPARATOR = new OverloadedFunctionComparator();

    private TranslationUtils() {
    }

    @NotNull
    public static JsPropertyInitializer translateFunctionAsEcma5PropertyDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext context) {
        if (JsDescriptorUtils.isExtension(descriptor)) {
            return translateExtensionFunctionAsEcma5DataDescriptor(function, descriptor, context);
        }
        else {
            JsStringLiteral getOrSet = context.program().getStringLiteral(descriptor instanceof PropertyGetterDescriptor ? "get" : "set");
            return new JsPropertyInitializer(getOrSet, function);
        }
    }

    @NotNull
    public static JsFunction simpleReturnFunction(@NotNull JsScope functionScope, @NotNull JsExpression returnExpression) {
        return new JsFunction(functionScope, new JsBlock(new JsReturn(returnExpression)));
    }

    @NotNull
    private static JsPropertyInitializer translateExtensionFunctionAsEcma5DataDescriptor(@NotNull JsFunction function,
            @NotNull FunctionDescriptor descriptor, @NotNull TranslationContext context) {
        JsObjectLiteral meta = createDataDescriptor(function, descriptor.getModality().isOverridable(), false);
        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), meta);
    }

    @NotNull
    public static JsExpression translateExclForBinaryEqualLikeExpr(@NotNull JsBinaryOperation baseBinaryExpression) {
        return new JsBinaryOperation(notOperator(baseBinaryExpression.getOperator()), baseBinaryExpression.getArg1(), baseBinaryExpression.getArg2());
    }

    public static boolean isEqualLikeOperator(@NotNull JsBinaryOperator operator) {
        return notOperator(operator) != null;
    }

    @Nullable
    private static JsBinaryOperator notOperator(@NotNull JsBinaryOperator operator) {
        switch (operator) {
            case REF_EQ:
                return REF_NEQ;
            case REF_NEQ:
                return REF_EQ;
            case EQ:
                return NEQ;
            case NEQ:
                return EQ;
            default:
                return null;
        }
    }

    @NotNull
    public static JsBinaryOperation isNullCheck(@NotNull JsExpression expressionToCheck) {
        return nullCheck(expressionToCheck, false);
    }

    @NotNull
    public static JsBinaryOperation isNotNullCheck(@NotNull JsExpression expressionToCheck) {
        return nullCheck(expressionToCheck, true);
    }

    @NotNull
    public static JsBinaryOperation nullCheck(@NotNull JsExpression expressionToCheck, boolean isNegated) {
        JsBinaryOperator operator = isNegated ? JsBinaryOperator.NEQ : JsBinaryOperator.EQ;
        return new JsBinaryOperation(operator, expressionToCheck, JsLiteral.NULL);
    }

    @NotNull
    public static JsConditional notNullConditional(
            @NotNull JsExpression expression,
            @NotNull JsExpression elseExpression,
            @NotNull TranslationContext context
    ) {
        JsExpression testExpression;
        JsExpression thenExpression;
        if (isCacheNeeded(expression)) {
            TemporaryConstVariable tempVar = context.getOrDeclareTemporaryConstVariable(expression);
            testExpression = isNotNullCheck(tempVar.value());
            thenExpression = tempVar.value();
        }
        else {
            testExpression = isNotNullCheck(expression);
            thenExpression = expression;
        }

        return new JsConditional(testExpression, thenExpression, elseExpression);
    }

    @NotNull
    public static String getMangledName(@NotNull PropertyDescriptor descriptor, @NotNull String suggestedName) {
        return getStableMangledName(suggestedName, getFqName(descriptor).asString());
    }

    @NotNull
    public static String getSuggestedName(@NotNull DeclarationDescriptor descriptor) {
        String suggestedName = descriptor.getName().asString();

        if (descriptor instanceof FunctionDescriptor) {
            suggestedName = getMangledName((FunctionDescriptor) descriptor);
        }

        return suggestedName;
    }

    @NotNull
    private static String getMangledName(@NotNull FunctionDescriptor descriptor) {
        if (needsStableMangling(descriptor)) {
            return getStableMangledName(descriptor);
        }

        return getSimpleMangledName(descriptor);
    }

    //TODO extend logic for nested/inner declarations
    private static boolean needsStableMangling(FunctionDescriptor descriptor) {
        // Use stable mangling for overrides because we use stable mangling when any function inside a overridable declaration
        // for avoid clashing names when inheritance.
        if (JsDescriptorUtils.isOverride(descriptor)) {
            return true;
        }

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();

        if (containingDeclaration instanceof PackageFragmentDescriptor) {
            return descriptor.getVisibility().isPublicAPI();
        }
        else if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

            // Use stable mangling when it inside a overridable declaration for avoid clashing names when inheritance.
            if (classDescriptor.getModality().isOverridable()) {
                return true;
            }

            // Don't use stable mangling when it inside a non-public API declaration.
            if (!classDescriptor.getVisibility().isPublicAPI()) {
                return false;
            }

            // Ignore the `protected` visibility because it can be use outside a containing declaration
            // only when the containing declaration is overridable.
            if (descriptor.getVisibility() == Visibilities.PUBLIC) {
                return true;
            }

            return false;
        }

        assert containingDeclaration instanceof CallableMemberDescriptor :
                "containingDeclaration for descriptor have unsupported type for mangling, " +
                "descriptor: " + descriptor + ", containingDeclaration: " + containingDeclaration;

        return false;
    }

    @NotNull
    public static String getMangledMemberNameForExplicitDelegation(@NotNull String suggestedName, FqName classFqName, FqName typeFqName) {
        String forCalculateId = classFqName.asString() + ":" + typeFqName.asString();
        return getStableMangledName(suggestedName, forCalculateId);
    }

    @NotNull
    private static String getStableMangledName(@NotNull String suggestedName, String forCalculateId) {
        int absHashCode = Math.abs(forCalculateId.hashCode());
        String suffix = absHashCode == 0 ? "" : ("_" + Integer.toString(absHashCode, Character.MAX_RADIX) + "$");
        return suggestedName + suffix;
    }

    @NotNull
    private static String getStableMangledName(@NotNull FunctionDescriptor descriptor) {
        return getStableMangledName(descriptor.getName().asString(), getArgumentTypesAsString(descriptor));
    }

    @NotNull
    private static String getSimpleMangledName(@NotNull final FunctionDescriptor descriptor) {
        DeclarationDescriptor declaration = descriptor.getContainingDeclaration();

        JetScope jetScope = null;
        if (declaration instanceof PackageFragmentDescriptor) {
            jetScope = ((PackageFragmentDescriptor) declaration).getMemberScope();
        }
        else if (declaration instanceof ClassDescriptor) {
            jetScope = ((ClassDescriptor) declaration).getDefaultType().getMemberScope();
        }

        int counter = 0;

        if (jetScope != null) {
            Collection<DeclarationDescriptor> declarations = jetScope.getAllDescriptors();
            List<FunctionDescriptor> overloadedFunctions = ContainerUtil.mapNotNull(declarations, new Function<DeclarationDescriptor, FunctionDescriptor>() {
                @Override
                public FunctionDescriptor fun(DeclarationDescriptor declarationDescriptor) {
                    if (!(declarationDescriptor instanceof FunctionDescriptor)) return null;

                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) declarationDescriptor;

                    String name = AnnotationsUtils.getNameForAnnotatedObjectWithOverrides(functionDescriptor);

                    // when name == null it's mean that it's not native.
                    if (name == null) {
                        // skip functions without arguments, because we don't use mangling for them
                        if (needsStableMangling(functionDescriptor) && !functionDescriptor.getValueParameters().isEmpty()) return null;

                        name = declarationDescriptor.getName().asString();
                    }

                    return descriptor.getName().asString().equals(name) ? functionDescriptor : null;
                }
            });

            if (overloadedFunctions.size() > 1) {
                Collections.sort(overloadedFunctions, OVERLOADED_FUNCTION_COMPARATOR);
                counter = ContainerUtil.indexOfIdentity(overloadedFunctions, descriptor);
                assert counter >= 0;
            }
        }

        String name = descriptor.getName().asString();
        return counter == 0 ? name : name + '_' + counter;
    }

    private static String getArgumentTypesAsString(FunctionDescriptor descriptor) {
        StringBuilder argTypes = new StringBuilder();

        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        if (receiverParameter != null) {
            argTypes.append(getJetTypeFqName(receiverParameter.getType())).append(".");
        }

        argTypes.append(StringUtil.join(descriptor.getValueParameters(), new Function<ValueParameterDescriptor, String>() {
            @Override
            public String fun(ValueParameterDescriptor descriptor) {
                return getJetTypeFqName(descriptor.getType());
            }
        }, ","));

        return argTypes.toString();
    }

    @NotNull
    public static String getJetTypeFqName(@NotNull JetType jetType) {
        ClassifierDescriptor declaration = jetType.getConstructor().getDeclarationDescriptor();
        assert declaration != null;

        if (declaration instanceof TypeParameterDescriptor) {
            return getJetTypeFqName(((TypeParameterDescriptor) declaration).getUpperBoundsAsType());
        }

        return getFqName(declaration).asString();
    }

    @NotNull
    public static JsNameRef backingFieldReference(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor) {
        JsName backingFieldName = context.getNameForDescriptor(descriptor);
        if(!JsDescriptorUtils.isSimpleFinalProperty(descriptor)) {
            String backingFieldMangledName;
            if (descriptor.getVisibility() != Visibilities.PRIVATE) {
                backingFieldMangledName = getMangledName(descriptor, getKotlinBackingFieldName(backingFieldName.getIdent()));
            } else {
                backingFieldMangledName = getKotlinBackingFieldName(backingFieldName.getIdent());
            }
            backingFieldName = context.declarePropertyOrPropertyAccessorName(descriptor, backingFieldMangledName, false);
        }
        return new JsNameRef(backingFieldName, JsLiteral.THIS);
    }

    @NotNull
    public static JsExpression assignmentToBackingField(@NotNull TranslationContext context,
            @NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression assignTo) {
        JsNameRef backingFieldReference = backingFieldReference(context, descriptor);
        return assignment(backingFieldReference, assignTo);
    }

    @Nullable
    public static JsExpression translateInitializerForProperty(@NotNull JetProperty declaration,
            @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = Translation.translateAsExpression(initializer, context);
        }
        return jsInitExpression;
    }

    @NotNull
    public static JsExpression translateBaseExpression(@NotNull TranslationContext context,
            @NotNull JetUnaryExpression expression) {
        JetExpression baseExpression = PsiUtils.getBaseExpression(expression);
        return Translation.translateAsExpression(baseExpression, context);
    }

    @NotNull
    public static JsExpression translateLeftExpression(@NotNull TranslationContext context,
            @NotNull JetBinaryExpression expression) {
        JetExpression left = expression.getLeft();
        assert left != null : "Binary expression should have a left expression: " + expression.getText();
        return Translation.translateAsExpression(left, context);
    }

    @NotNull
    public static JsExpression translateRightExpression(@NotNull TranslationContext context,
            @NotNull JetBinaryExpression expression) {
        JetExpression rightExpression = expression.getRight();
        assert rightExpression != null : "Binary expression should have a right expression";
        return Translation.translateAsExpression(rightExpression, context);
    }

    public static boolean hasCorrespondingFunctionIntrinsic(@NotNull TranslationContext context,
            @NotNull JetOperationExpression expression) {
        CallableDescriptor operationDescriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);

        if (operationDescriptor == null || !(operationDescriptor instanceof FunctionDescriptor)) return true;
        if (context.intrinsics().getFunctionIntrinsics().getIntrinsic((FunctionDescriptor) operationDescriptor).exists()) return true;

        return false;
    }

    @NotNull
    public static List<JsExpression> generateInvocationArguments(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments) {
        if (arguments.isEmpty()) {
            return Collections.singletonList(receiver);
        }

        List<JsExpression> argumentList = new ArrayList<JsExpression>(1 + arguments.size());
        argumentList.add(receiver);
        argumentList.addAll(arguments);
        return argumentList;
    }

    public static boolean isCacheNeeded(@NotNull JsExpression expression) {
        return !(expression instanceof JsLiteral) &&
               (!(expression instanceof JsNameRef) || ((JsNameRef) expression).getQualifier() != null);
    }

    @NotNull
    public static Pair<JsVars.JsVar, JsExpression> createTemporaryIfNeed(
            @NotNull JsExpression expression,
            @NotNull TranslationContext context
    ) {
        // don't create temp variable for simple expression
        if (isCacheNeeded(expression)) {
            return context.dynamicContext().createTemporary(expression);
        }
        else {
            return Pair.create(null, expression);
        }
    }

    @NotNull
    public static JsConditional sure(@NotNull JsExpression expression, @NotNull TranslationContext context) {
        JsInvocation throwNPE = new JsInvocation(context.namer().throwNPEFunctionRef());
        JsConditional ensureNotNull = notNullConditional(expression, throwNPE, context);

        JsExpression thenExpression = ensureNotNull.getThenExpression();
        if (thenExpression instanceof JsNameRef) {
            // associate (cache) ensureNotNull expression to new TemporaryConstVariable with same name.
            context.associateExpressionToLazyValue(ensureNotNull,
                                                   new TemporaryConstVariable(((JsNameRef) thenExpression).getName(), ensureNotNull));
        }

        return ensureNotNull;
    }

    @NotNull
    public static String getSuggestedNameForInnerDeclaration(TranslationContext context, DeclarationDescriptor descriptor) {
        String suggestedName = "";
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration != null &&
            !(containingDeclaration instanceof ClassOrPackageFragmentDescriptor) &&
            !(containingDeclaration instanceof AnonymousFunctionDescriptor)) {
            suggestedName = context.getNameForDescriptor(containingDeclaration).getIdent();
        }

        if (!suggestedName.isEmpty() && !suggestedName.endsWith("$")) {
            suggestedName += "$";
        }

        if (descriptor.getName().isSpecial()) {
            suggestedName += "f";
        }
        else {
            suggestedName += context.getNameForDescriptor(descriptor).getIdent();
        }
        return suggestedName;
    }

    private static class OverloadedFunctionComparator implements Comparator<FunctionDescriptor> {
        @Override
        public int compare(@NotNull FunctionDescriptor a, @NotNull FunctionDescriptor b) {
            // native functions first
            if (isNativeOrOverrideNative(a)) {
                if (!isNativeOrOverrideNative(b)) return -1;
            }
            else if (isNativeOrOverrideNative(b)) {
                return 1;
            }

            // be visibility
            // Actually "internal" > "private", but we want to have less number for "internal", so compare b with a instead of a with b.
            Integer result = Visibilities.compare(b.getVisibility(), a.getVisibility());
            if (result != null && result != 0) return result;

            // by arity
            int aArity = arity(a);
            int bArity = arity(b);
            if (aArity != bArity) return aArity - bArity;

            // by stringify argument types
            String aArguments = getArgumentTypesAsString(a);
            String bArguments = getArgumentTypesAsString(b);
            assert aArguments != bArguments;

            return aArguments.compareTo(bArguments);
        }

        private static int arity(FunctionDescriptor descriptor) {
            return descriptor.getValueParameters().size() + (descriptor.getReceiverParameter() == null ? 0 : 1);
        }

        private static boolean isNativeOrOverrideNative(FunctionDescriptor descriptor) {
            if (AnnotationsUtils.isNativeObject(descriptor)) return true;

            Set<FunctionDescriptor> declarations = OverrideResolver.getAllOverriddenDeclarations(descriptor);
            for (FunctionDescriptor memberDescriptor : declarations) {
                if (AnnotationsUtils.isNativeObject(memberDescriptor)) return true;
            }
            return false;
        }
    }
}
