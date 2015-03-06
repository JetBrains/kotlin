/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.exceptions.MainFunctionNotFoundException;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationInternalException;
import org.jetbrains.kotlin.js.facade.exceptions.UnsupportedFeatureException;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.StaticContext;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.PackageDeclarationTranslator;
import org.jetbrains.kotlin.js.translate.expression.ExpressionVisitor;
import org.jetbrains.kotlin.js.translate.expression.FunctionTranslator;
import org.jetbrains.kotlin.js.translate.expression.PatternTranslator;
import org.jetbrains.kotlin.js.translate.test.JSRhinoUnitTester;
import org.jetbrains.kotlin.js.translate.test.JSTestGenerator;
import org.jetbrains.kotlin.js.translate.test.JSTester;
import org.jetbrains.kotlin.js.translate.test.QUnitTester;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.kotlin.psi.JetDeclarationWithBody;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    public static FunctionTranslator functionTranslator(@NotNull JetDeclarationWithBody function,
            @NotNull TranslationContext context) {
        return FunctionTranslator.newInstance(function, context);
    }

    @NotNull
    public static PatternTranslator patternTranslator(@NotNull TranslationContext context) {
        return PatternTranslator.newInstance(context);
    }

    @NotNull
    public static JsNode translateExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return translateExpression(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsNode translateExpression(@NotNull JetExpression expression, @NotNull TranslationContext context, @NotNull JsBlock block) {
        JsExpression aliasForExpression = context.aliasingContext().getAliasForExpression(expression);
        if (aliasForExpression != null) {
            return aliasForExpression;
        }

        TranslationContext innerContext = context.innerBlock();
        JsNode result = doTranslateExpression(expression, innerContext);
        context.moveVarsFrom(innerContext);
        block.getStatements().addAll(innerContext.dynamicContext().jsBlock().getStatements());

        if (BindingContextUtilPackage.isUnreachableCode(expression, context.bindingContext())) {
            return context.getEmptyExpression();
        }

        return result;
    }

    //NOTE: use with care
    @NotNull
    public static JsNode doTranslateExpression(JetExpression expression, TranslationContext context) {
        return expression.accept(new ExpressionVisitor(), context);
    }

    @NotNull
    public static JsExpression translateAsExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return translateAsExpression(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsExpression translateAsExpression(
            @NotNull JetExpression expression,
            @NotNull TranslationContext context,
            @NotNull JsBlock block
    ) {
        JsNode jsNode = translateExpression(expression, context, block);
        if (jsNode instanceof  JsExpression) {
            return (JsExpression) jsNode;
        }

        assert jsNode instanceof JsStatement : "Unexpected node of type: " + jsNode.getClass().toString();
        if (BindingContextUtilPackage.isUsedAsExpression(expression, context.bindingContext())) {
            TemporaryVariable result = context.declareTemporary(null);
            AssignToExpressionMutator saveResultToTemporaryMutator = new AssignToExpressionMutator(result.reference());
            block.getStatements().add(mutateLastExpression(jsNode, saveResultToTemporaryMutator));
            return result.reference();
        }

        block.getStatements().add(convertToStatement(jsNode));
        return context.getEmptyExpression();
    }

    @NotNull
    public static JsStatement translateAsStatement(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        return translateAsStatement(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsStatement translateAsStatement(
            @NotNull JetExpression expression,
            @NotNull TranslationContext context,
            @NotNull JsBlock block) {
        return convertToStatement(translateExpression(expression, context, block));
    }

    @NotNull
    public static JsStatement translateAsStatementAndMergeInBlockIfNeeded(
            @NotNull JetExpression expression,
            @NotNull TranslationContext context
    ) {
        JsBlock block = new JsBlock();
        JsNode node = translateExpression(expression, context, block);
        return JsAstUtils.mergeStatementInBlockIfNeeded(convertToStatement(node), block);
    }

    @NotNull
    public static TranslationContext generateAst(@NotNull BindingTrace bindingTrace,
            @NotNull Collection<JetFile> files, @NotNull MainCallParameters mainCallParameters,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull Config config)
            throws TranslationException {
        try {
            return doGenerateAst(bindingTrace, files, mainCallParameters, moduleDescriptor, config);
        }
        catch (UnsupportedOperationException e) {
            throw new UnsupportedFeatureException("Unsupported feature used.", e);
        }
        catch (Throwable e) {
            throw new TranslationInternalException(e);
        }
    }

    @NotNull
    private static TranslationContext doGenerateAst(@NotNull BindingTrace bindingTrace, @NotNull Collection<JetFile> files,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull Config config) throws MainFunctionNotFoundException {
        StaticContext staticContext = StaticContext.generateStaticContext(bindingTrace, config, moduleDescriptor);
        JsProgram program = staticContext.getProgram();
        JsBlock block = program.getGlobalBlock();

        JsFunction rootFunction = JsAstUtils.createPackage(block.getStatements(), program.getScope());
        JsBlock rootBlock = rootFunction.getBody();
        List<JsStatement> statements = rootBlock.getStatements();
        statements.add(program.getStringLiteral("use strict").makeStmt());

        TranslationContext context = TranslationContext.rootContext(staticContext, rootFunction);
        statements.addAll(PackageDeclarationTranslator.translateFiles(files, context));
        defineModule(context, statements, config.getModuleId());

        if (mainCallParameters.shouldBeGenerated()) {
            JsStatement statement = generateCallToMain(context, files, mainCallParameters.arguments());
            if (statement != null) {
                statements.add(statement);
            }
        }
        mayBeGenerateTests(files, config, rootBlock, context);
        return context;
    }

    private static void defineModule(@NotNull TranslationContext context, @NotNull List<JsStatement> statements, @NotNull String moduleId) {
        JsName rootPackageName = context.scope().findName(Namer.getRootPackageName());
        if (rootPackageName != null) {
            statements.add(new JsInvocation(context.namer().kotlin("defineModule"), context.program().getStringLiteral(moduleId),
                                            rootPackageName.makeRef()).makeStmt());
        }
    }

    private static void mayBeGenerateTests(@NotNull Collection<JetFile> files, @NotNull Config config,
            @NotNull JsBlock rootBlock, @NotNull TranslationContext context) {
        JSTester tester = config.isTestConfig() ? new JSRhinoUnitTester() : new QUnitTester();
        tester.initialize(context, rootBlock);
        JSTestGenerator.generateTestCalls(context, files, tester);
        tester.deinitialize();
    }

    //TODO: determine whether should throw exception
    @Nullable
    private static JsStatement generateCallToMain(@NotNull TranslationContext context, @NotNull Collection<JetFile> files,
            @NotNull List<String> arguments) throws MainFunctionNotFoundException {
        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(context.bindingContext());
        JetNamedFunction mainFunction = mainFunctionDetector.getMainFunction(files);
        if (mainFunction == null) {
            return null;
        }
        FunctionDescriptor functionDescriptor = getFunctionDescriptor(context.bindingContext(), mainFunction);
        JsArrayLiteral argument = new JsArrayLiteral(toStringLiteralList(arguments, context.program()));
        return CallTranslator.INSTANCE$.buildCall(context, functionDescriptor, Collections.singletonList(argument), null).makeStmt();
    }
}
