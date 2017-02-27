/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.general;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationRuntimeException;
import org.jetbrains.kotlin.js.facade.exceptions.UnsupportedFeatureException;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.StaticContext;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.PackageDeclarationTranslator;
import org.jetbrains.kotlin.js.translate.expression.ExpressionVisitor;
import org.jetbrains.kotlin.js.translate.expression.PatternTranslator;
import org.jetbrains.kotlin.js.translate.test.JSRhinoUnitTester;
import org.jetbrains.kotlin.js.translate.test.JSTestGenerator;
import org.jetbrains.kotlin.js.translate.test.JSTester;
import org.jetbrains.kotlin.js.translate.test.QUnitTester;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtUnaryExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.NullValue;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.general.ModuleWrapperTranslation.wrapIfNecessary;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.convertToStatement;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.toStringLiteralList;
import static org.jetbrains.kotlin.js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;

/**
 * This class provides a interface which all translators use to interact with each other.
 * Goal is to simplify interaction between translators.
 */
public final class Translation {

    private Translation() {
    }

    @NotNull
    public static PatternTranslator patternTranslator(@NotNull TranslationContext context) {
        return PatternTranslator.newInstance(context);
    }

    @NotNull
    public static JsNode translateExpression(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        return translateExpression(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsNode translateExpression(@NotNull KtExpression expression, @NotNull TranslationContext context, @NotNull JsBlock block) {
        JsExpression aliasForExpression = context.aliasingContext().getAliasForExpression(expression);
        if (aliasForExpression != null) {
            return aliasForExpression;
        }

        CompileTimeConstant<?> compileTimeValue = ConstantExpressionEvaluator.getConstant(expression, context.bindingContext());
        if (compileTimeValue != null) {
            KotlinType type = context.bindingContext().getType(expression);
            if (type != null) {
                if (KotlinBuiltIns.isLong(type) || (KotlinBuiltIns.isInt(type) && expression instanceof KtUnaryExpression)) {
                    JsExpression constantResult = translateConstant(compileTimeValue, expression, context);
                    if (constantResult != null) return constantResult;
                }
            }
        }

        TranslationContext innerContext = context.innerBlock();
        JsNode result = doTranslateExpression(expression, innerContext);
        context.moveVarsFrom(innerContext);
        block.getStatements().addAll(innerContext.dynamicContext().jsBlock().getStatements());

        return result;
    }

    @Nullable
    public static JsExpression translateConstant(
            @NotNull CompileTimeConstant compileTimeValue,
            @NotNull KtExpression expression,
            @NotNull TranslationContext context
    ) {
        KotlinType expectedType = context.bindingContext().getType(expression);
        ConstantValue<?> constant = compileTimeValue.toConstantValue(expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE);
        if (constant instanceof NullValue) {
            return JsLiteral.NULL;
        }
        Object value = constant.getValue();
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return context.program().getNumberLiteral(((Number) value).intValue());
        }
        else if (value instanceof Long) {
            return JsAstUtils.newLong((Long) value, context);
        }
        else if (value instanceof Float) {
            float floatValue = (Float) value;
            double doubleValue;
            if (Float.isInfinite(floatValue) || Float.isNaN(floatValue)) {
                doubleValue = floatValue;
            }
            else {
                doubleValue = Double.parseDouble(Float.toString(floatValue));
            }
            return context.program().getNumberLiteral(doubleValue);
        }
        else if (value instanceof Number) {
            return context.program().getNumberLiteral(((Number) value).doubleValue());
        }
        else if (value instanceof Boolean) {
            return JsLiteral.getBoolean((Boolean) value);
        }

        //TODO: test
        if (value instanceof String) {
            return context.program().getStringLiteral((String) value);
        }
        if (value instanceof Character) {
            return context.program().getNumberLiteral(((Character) value).charValue());
        }

        return null;
    }

    @NotNull
    private static JsNode doTranslateExpression(KtExpression expression, TranslationContext context) {
        try {
            return expression.accept(new ExpressionVisitor(), context);
        }
        catch (TranslationRuntimeException e) {
            throw e;
        }
        catch (RuntimeException e) {
            throw new TranslationRuntimeException(expression, e);
        }
        catch (AssertionError e) {
            throw new TranslationRuntimeException(expression, e);
        }
    }

    @NotNull
    public static JsExpression translateAsExpression(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        return translateAsExpression(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsExpression translateAsExpression(
            @NotNull KtExpression expression,
            @NotNull TranslationContext context,
            @NotNull JsBlock block
    ) {
        JsNode jsNode = translateExpression(expression, context, block);
        if (jsNode instanceof  JsExpression) {
            KotlinType expressionType = context.bindingContext().getType(expression);
            return unboxIfNeeded((JsExpression) jsNode, expressionType != null && KotlinBuiltIns.isCharOrNullableChar(expressionType));
        }

        assert jsNode instanceof JsStatement : "Unexpected node of type: " + jsNode.getClass().toString();
        if (BindingContextUtilsKt.isUsedAsExpression(expression, context.bindingContext())) {
            TemporaryVariable result = context.declareTemporary(null);
            AssignToExpressionMutator saveResultToTemporaryMutator = new AssignToExpressionMutator(result.reference());
            block.getStatements().add(mutateLastExpression(jsNode, saveResultToTemporaryMutator));
            return result.reference();
        }

        block.getStatements().add(convertToStatement(jsNode));
        return JsLiteral.NULL;
    }

    @NotNull
    public static JsExpression unboxIfNeeded(@NotNull JsExpression expression, boolean charOrNullableChar) {
        if (charOrNullableChar &&
            (expression instanceof JsInvocation || expression instanceof JsNameRef || expression instanceof JsArrayAccess)
        ) {
            expression = JsAstUtils.boxedCharToChar(expression);
        }
        return expression;
    }

    @NotNull
    public static JsStatement translateAsStatement(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        return translateAsStatement(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsStatement translateAsStatement(
            @NotNull KtExpression expression,
            @NotNull TranslationContext context,
            @NotNull JsBlock block) {
        return convertToStatement(translateExpression(expression, context, block));
    }

    @NotNull
    public static JsStatement translateAsStatementAndMergeInBlockIfNeeded(
            @NotNull KtExpression expression,
            @NotNull TranslationContext context
    ) {
        JsBlock block = new JsBlock();
        JsNode node = translateExpression(expression, context, block);
        return JsAstUtils.mergeStatementInBlockIfNeeded(convertToStatement(node), block);
    }

    @NotNull
    public static TranslationContext generateAst(
            @NotNull BindingTrace bindingTrace,
            @NotNull Collection<KtFile> files,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull JsConfig config
    ) throws TranslationException {
        try {
            return doGenerateAst(bindingTrace, files, mainCallParameters, moduleDescriptor, config);
        }
        catch (UnsupportedOperationException e) {
            throw new UnsupportedFeatureException("Unsupported feature used.", e);
        }
        catch (Throwable e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    private static TranslationContext doGenerateAst(
            @NotNull BindingTrace bindingTrace,
            @NotNull Collection<KtFile> files,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull JsConfig config
    ) {
        StaticContext staticContext = StaticContext.generateStaticContext(bindingTrace, config, moduleDescriptor);
        JsProgram program = staticContext.getProgram();
        JsName rootPackageName = program.getRootScope().declareName(Namer.getRootPackageName());

        JsFunction rootFunction = staticContext.getRootFunction();
        JsBlock rootBlock = rootFunction.getBody();
        List<JsStatement> statements = rootBlock.getStatements();

        program.getScope().declareName("_");

        TranslationContext context = TranslationContext.rootContext(staticContext, rootFunction);
        PackageDeclarationTranslator.translateFiles(files, context);
        staticContext.postProcess();
        statements.add(0, program.getStringLiteral("use strict").makeStmt());
        if (!staticContext.isBuiltinModule()) {
            defineModule(context, statements, config.getModuleId());
        }

        mayBeGenerateTests(files, config, rootBlock, context);
        rootFunction.getParameters().add(new JsParameter((rootPackageName)));

        // Invoke function passing modules as arguments
        // This should help minifier tool to recognize references to these modules as local variables and make them shorter.
        List<StaticContext.ImportedModule> importedModuleList = new ArrayList<StaticContext.ImportedModule>();

        for (StaticContext.ImportedModule importedModule : staticContext.getImportedModules()) {
            rootFunction.getParameters().add(new JsParameter(importedModule.getInternalName()));
            importedModuleList.add(importedModule);
        }

        if (mainCallParameters.shouldBeGenerated()) {
            JsStatement statement = generateCallToMain(context, files, mainCallParameters.arguments());
            if (statement != null) {
                statements.add(statement);
            }
        }

        statements.add(new JsReturn(rootPackageName.makeRef()));

        JsBlock block = program.getGlobalBlock();
        block.getStatements().addAll(wrapIfNecessary(config.getModuleId(), rootFunction, importedModuleList, program,
                                                     config.getModuleKind()));

        return context;
    }

    private static void defineModule(@NotNull TranslationContext context, @NotNull List<JsStatement> statements, @NotNull String moduleId) {
        JsName rootPackageName = context.scope().findName(Namer.getRootPackageName());
        if (rootPackageName != null) {
            statements.add(new JsInvocation(context.namer().kotlin("defineModule"), context.program().getStringLiteral(moduleId),
                                            rootPackageName.makeRef()).makeStmt());
        }
    }

    private static void mayBeGenerateTests(
            @NotNull Collection<KtFile> files, @NotNull JsConfig config, @NotNull JsBlock rootBlock, @NotNull TranslationContext context
    ) {
        JSTester tester =
                config.getConfiguration().getBoolean(JSConfigurationKeys.UNIT_TEST_CONFIG) ? new JSRhinoUnitTester() : new QUnitTester();
        tester.initialize(context, rootBlock);
        JSTestGenerator.generateTestCalls(context, files, tester);
        tester.deinitialize();
    }

    //TODO: determine whether should throw exception
    @Nullable
    private static JsStatement generateCallToMain(
            @NotNull TranslationContext context, @NotNull Collection<KtFile> files, @NotNull List<String> arguments
    ) {
        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(context.bindingContext());
        KtNamedFunction mainFunction = mainFunctionDetector.getMainFunction(files);
        if (mainFunction == null) {
            return null;
        }
        FunctionDescriptor functionDescriptor = getFunctionDescriptor(context.bindingContext(), mainFunction);
        JsArrayLiteral argument = new JsArrayLiteral(toStringLiteralList(arguments, context.program()));
        return CallTranslator.INSTANCE.buildCall(context, functionDescriptor, Collections.singletonList(argument), null).makeStmt();
    }
}
