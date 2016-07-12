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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
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
import org.jetbrains.kotlin.js.translate.expression.FunctionTranslator;
import org.jetbrains.kotlin.js.translate.expression.PatternTranslator;
import org.jetbrains.kotlin.js.translate.test.JSRhinoUnitTester;
import org.jetbrains.kotlin.js.translate.test.JSTestGenerator;
import org.jetbrains.kotlin.js.translate.test.JSTester;
import org.jetbrains.kotlin.js.translate.test.QUnitTester;
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.kotlin.psi.KtDeclarationWithBody;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.serialization.js.ModuleKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.util.ArrayList;
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
    public static FunctionTranslator functionTranslator(@NotNull KtDeclarationWithBody function,
            @NotNull TranslationContext context) {
        return FunctionTranslator.newInstance(function, context);
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

        TranslationContext innerContext = context.innerBlock();
        JsNode result = doTranslateExpression(expression, innerContext);
        context.moveVarsFrom(innerContext);
        block.getStatements().addAll(innerContext.dynamicContext().jsBlock().getStatements());

        return result;
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
            return (JsExpression) jsNode;
        }

        assert jsNode instanceof JsStatement : "Unexpected node of type: " + jsNode.getClass().toString();
        if (BindingContextUtilsKt.isUsedAsExpression(expression, context.bindingContext())) {
            TemporaryVariable result = context.declareTemporary(null);
            AssignToExpressionMutator saveResultToTemporaryMutator = new AssignToExpressionMutator(result.reference());
            block.getStatements().add(mutateLastExpression(jsNode, saveResultToTemporaryMutator));
            return result.reference();
        }

        block.getStatements().add(convertToStatement(jsNode));
        return context.getEmptyExpression();
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

    private static class ImportedModule {
        final String id;
        final JsName name;

        public ImportedModule(String id, JsName name) {
            this.id = id;
            this.name = name;
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

        JsFunction rootFunction = JsAstUtils.createFunctionWithEmptyBody(program.getScope());
        JsBlock rootBlock = rootFunction.getBody();
        List<JsStatement> statements = rootBlock.getStatements();
        statements.add(program.getStringLiteral("use strict").makeStmt());

        TranslationContext context = TranslationContext.rootContext(staticContext, rootFunction);
        statements.addAll(PackageDeclarationTranslator.translateFiles(files, context));
        defineModule(context, statements, config.getModuleId());

        mayBeGenerateTests(files, config, rootBlock, context);

        // Invoke function passing modules as arguments
        // This should help minifier tool to recognize references to this modules as local variables and make them shorter.
        List<ImportedModule> importedModuleList = new ArrayList<ImportedModule>();
        JsName kotlinName = program.getScope().declareName(Namer.KOTLIN_NAME);
        rootFunction.getParameters().add(new JsParameter((kotlinName)));
        importedModuleList.add(new ImportedModule(Namer.KOTLIN_LOWER_NAME, kotlinName));

        for (String importedModule : staticContext.getImportedModules().keySet()) {
            rootFunction.getParameters().add(new JsParameter(staticContext.getImportedModules().get(importedModule)));
            JsName globalId = program.getScope().declareName(Namer.suggestedModuleName(importedModule));
            importedModuleList.add(new ImportedModule(importedModule, globalId));
        }

        if (mainCallParameters.shouldBeGenerated()) {
            JsStatement statement = generateCallToMain(context, files, mainCallParameters.arguments());
            if (statement != null) {
                statements.add(statement);
            }
        }

        statements.add(new JsReturn(program.getRootScope().declareName(Namer.getRootPackageName()).makeRef()));

        JsBlock block = program.getGlobalBlock();
        block.getStatements().addAll(wrapIfNecessary(config.getModuleId(), rootFunction, importedModuleList, program,
                                                     config.getModuleKind()));

        return context;
    }

    @NotNull
    private static List<JsStatement> wrapIfNecessary(
            @NotNull String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program,
            @NotNull ModuleKind kind
    ) {
        switch (kind) {
            case AMD:
                return wrapAmd(moduleId, function, importedModules, program);
            case COMMON_JS:
                return wrapCommonJs(function, importedModules, program);
            case UMD:
                return wrapUmd(moduleId, function, importedModules, program);
            case PLAIN:
            default:
                return wrapPlain(moduleId, function, importedModules, program);
        }
    }

    @NotNull
    private static List<JsStatement> wrapUmd(
            @Nullable String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsScope scope = program.getScope();
        JsName rootName = scope.declareName("root");
        JsName factoryName = scope.declareName("factory");
        JsName defineName = scope.declareName("define");
        JsName exportsName = scope.declareName("exports");

        JsExpression amdTest = JsAstUtils.and(JsAstUtils.typeOfIs(defineName.makeRef(), program.getStringLiteral("function")),
                                              new JsNameRef("amd", defineName.makeRef()));
        JsExpression commonJsTest = JsAstUtils.typeOfIs(exportsName.makeRef(), program.getStringLiteral("object"));

        JsBlock amdBody = new JsBlock(wrapAmd(moduleId, factoryName.makeRef(), importedModules, program));
        JsBlock commonJsBody = new JsBlock(wrapCommonJs(factoryName.makeRef(), importedModules, program));
        JsInvocation plainInvocation = makePlainInvocation(factoryName.makeRef(), importedModules, program);
        JsExpression plainExpr = moduleId != null ?
                                 JsAstUtils.assignment(new JsNameRef(moduleId, rootName.makeRef()), plainInvocation) :
                                 plainInvocation;

        JsStatement selector = JsAstUtils.newJsIf(amdTest, amdBody, JsAstUtils.newJsIf(commonJsTest, commonJsBody, plainExpr.makeStmt()));
        JsFunction adapter = new JsFunction(program.getScope(), new JsBlock(selector), "UMD adapter");
        adapter.getParameters().add(new JsParameter(rootName));
        adapter.getParameters().add(new JsParameter(factoryName));

        return Collections.singletonList(new JsInvocation(adapter, JsLiteral.THIS, function).makeStmt());
    }

    @NotNull
    private static List<JsStatement> wrapAmd(
            @Nullable String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsScope scope = program.getScope();
        JsName defineName = scope.declareName("define");
        List<JsExpression> invocationArgs = new ArrayList<JsExpression>();

        if (moduleId != null) {
            invocationArgs.add(program.getStringLiteral(moduleId));
        }

        List<JsExpression> moduleNameList = new ArrayList<JsExpression>(importedModules.size());
        for (ImportedModule importedModule : importedModules) {
            moduleNameList.add(program.getStringLiteral(importedModule.id));
        }
        invocationArgs.add(new JsArrayLiteral(moduleNameList));

        invocationArgs.add(function);

        JsInvocation invocation = new JsInvocation(defineName.makeRef(), invocationArgs);
        return Collections.singletonList(invocation.makeStmt());
    }

    @NotNull
    private static List<JsStatement> wrapCommonJs(
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsScope scope = program.getScope();
        JsName moduleName = scope.declareName("module");
        JsName requireName = scope.declareName("require");

        List<JsExpression> invocationArgs = new ArrayList<JsExpression>();
        for (ImportedModule importedModule : importedModules) {
            invocationArgs.add(new JsInvocation(requireName.makeRef(), program.getStringLiteral(importedModule.id)));
        }

        JsInvocation invocation = new JsInvocation(function, invocationArgs);
        JsExpression assignment = JsAstUtils.assignment(new JsNameRef("exports", moduleName.makeRef()), invocation);
        return Collections.singletonList(assignment.makeStmt());
    }

    @NotNull
    private static List<JsStatement> wrapPlain(
            @Nullable String moduleId,
            @NotNull JsExpression function,
            @NotNull List<ImportedModule> importedModules,
            @NotNull JsProgram program
    ) {
        JsInvocation invocation = makePlainInvocation(function, importedModules, program);

        JsStatement statement;
        if (moduleId == null) {
            statement = invocation.makeStmt();
        }
        else {
            statement = JsAstUtils.newVar(program.getScope().declareName(moduleId), invocation);
        }

        return Collections.singletonList(statement);
    }

    @NotNull
    private static JsInvocation makePlainInvocation(@NotNull JsExpression function, @NotNull List<ImportedModule> importedModules,
            JsProgram program) {
        List<JsExpression> invocationArgs = new ArrayList<JsExpression>(importedModules.size());

        for (ImportedModule importedModule : importedModules) {
            invocationArgs.add(program.getRootScope().declareName(importedModule.id).makeRef());
        }

        return new JsInvocation(function, invocationArgs);
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
