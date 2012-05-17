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

package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.JsNamer;
import com.google.dart.compiler.backend.js.JsPrettyNamer;
import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.facade.exceptions.MainFunctionNotFoundException;
import org.jetbrains.k2js.facade.exceptions.TranslationException;
import org.jetbrains.k2js.facade.exceptions.TranslationInternalException;
import org.jetbrains.k2js.facade.exceptions.UnsupportedFeatureException;
import org.jetbrains.k2js.translate.context.StaticContext;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.declaration.NamespaceDeclarationTranslator;
import org.jetbrains.k2js.translate.expression.ExpressionVisitor;
import org.jetbrains.k2js.translate.expression.FunctionTranslator;
import org.jetbrains.k2js.translate.expression.PatternTranslator;
import org.jetbrains.k2js.translate.expression.WhenTranslator;
import org.jetbrains.k2js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.k2js.translate.initializer.NamespaceInitializerTranslator;
import org.jetbrains.k2js.translate.reference.CallBuilder;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.dangerous.DangerousData;
import org.jetbrains.k2js.translate.utils.dangerous.DangerousTranslator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.plugin.JetMainDetector.getMainFunction;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;
import static org.jetbrains.k2js.translate.utils.dangerous.DangerousData.collect;

/**
 * @author Pavel Talanov
 *         <p/>
 *         This class provides a interface which all translators use to interact with each other.
 *         Goal is to simplify interaction between translators.
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
    public static List<JsStatement> translateFiles(@NotNull List<JetFile> files, @NotNull TranslationContext context) {
        return NamespaceDeclarationTranslator.translateFiles(files, context);
    }

    @NotNull
    public static JsInvocation translateClassDeclaration(@NotNull JetClass classDeclaration,
            @NotNull Map<JsName, JsName> aliasingMap,
            @NotNull TranslationContext context) {
        return ClassTranslator.generateClassCreationExpression(classDeclaration, aliasingMap, context);
    }

    @NotNull
    public static PatternTranslator patternTranslator(@NotNull TranslationContext context) {
        return PatternTranslator.newInstance(context);
    }

    @NotNull
    public static JsNode translateExpression(@NotNull JetExpression expression, @NotNull TranslationContext context) {
        JsName aliasForExpression = context.aliasingContext().getAliasForExpression(expression);
        if (aliasForExpression != null) {
            return aliasForExpression.makeRef();
        }
        DangerousData data = collect(expression, context);
        if (data.shouldBeTranslated()) {
            return DangerousTranslator.translate(data, context);
        }
        return doTranslateExpression(expression, context);
    }

    //NOTE: use with care
    @NotNull
    public static JsNode doTranslateExpression(JetExpression expression, TranslationContext context) {
        return expression.accept(new ExpressionVisitor(), context);
    }

    @NotNull
    public static JsExpression translateAsExpression(@NotNull JetExpression expression,
            @NotNull TranslationContext context) {
        return convertToExpression(translateExpression(expression, context));
    }

    @NotNull
    public static JsStatement translateAsStatement(@NotNull JetExpression expression,
            @NotNull TranslationContext context) {
        return convertToStatement(translateExpression(expression, context));
    }

    @NotNull
    public static JsNode translateWhenExpression(@NotNull JetWhenExpression expression,
            @NotNull TranslationContext context) {
        return WhenTranslator.translateWhenExpression(expression, context);
    }

    //TODO: see if generate*Initializer methods fit somewhere else
    @NotNull
    public static JsPropertyInitializer generateClassInitializerMethod(@NotNull JetClassOrObject classDeclaration,
            @NotNull TranslationContext context) {
        final ClassInitializerTranslator classInitializerTranslator = new ClassInitializerTranslator(classDeclaration, context);
        return classInitializerTranslator.generateInitializeMethod();
    }

    @NotNull
    public static JsPropertyInitializer generateNamespaceInitializerMethod(@NotNull NamespaceDescriptor namespace,
            @NotNull TranslationContext context) {
        final NamespaceInitializerTranslator namespaceInitializerTranslator = new NamespaceInitializerTranslator(namespace, context);
        return namespaceInitializerTranslator.generateInitializeMethod();
    }

    @NotNull
    public static JsProgram generateAst(@NotNull BindingContext bindingContext,
            @NotNull List<JetFile> files, @NotNull MainCallParameters mainCallParameters,
            @NotNull EcmaVersion ecmaVersion)
            throws TranslationException {
        try {
            return doGenerateAst(bindingContext, files, mainCallParameters, ecmaVersion);
        }
        catch (UnsupportedOperationException e) {
            throw new UnsupportedFeatureException("Unsupported feature used.", e);
        }
        catch (Throwable e) {
            throw new TranslationInternalException(e);
        }
    }

    @NotNull
    private static JsProgram doGenerateAst(@NotNull BindingContext bindingContext, @NotNull List<JetFile> files,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull EcmaVersion ecmaVersion) throws MainFunctionNotFoundException {
        //TODO: move some of the code somewhere
        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();
        StaticContext staticContext = StaticContext.generateStaticContext(standardLibrary, bindingContext, ecmaVersion);
        JsBlock block = staticContext.getProgram().getGlobalBlock();

        List<JsStatement> statements = JsAstUtils.createPackage(block.getStatements(), staticContext.getProgram().getRootScope());
        if (ecmaVersion == EcmaVersion.v5) {
            statements.add(staticContext.getProgram().getStringLiteral("use strict").makeStmt());
        }

        TranslationContext context = TranslationContext.rootContext(staticContext);
        statements.addAll(translateFiles(files, context));
        if (mainCallParameters.shouldBeGenerated()) {
            statements.add(generateCallToMain(context, files, mainCallParameters.arguments()));
        }
        JsNamer namer = new JsPrettyNamer();
        namer.exec(context.program());
        return context.program();
    }

    @NotNull
    private static JsStatement generateCallToMain(@NotNull TranslationContext context, @NotNull List<JetFile> files,
            @NotNull List<String> arguments) throws MainFunctionNotFoundException {
        JetNamedFunction mainFunction = getMainFunction(files);
        if (mainFunction == null) {
            throw new MainFunctionNotFoundException("Main function was not found. Please check compiler arguments");
        }
        JsInvocation translatedCall = generateInvocation(context, mainFunction);
        setArguments(context, arguments, translatedCall);
        return translatedCall.makeStmt();
    }

    @NotNull
    private static JsInvocation generateInvocation(@NotNull TranslationContext context, @NotNull JetNamedFunction mainFunction) {
        FunctionDescriptor functionDescriptor = getFunctionDescriptor(context.bindingContext(), mainFunction);
        JsExpression translatedCall = CallBuilder.build(context).descriptor(functionDescriptor).translate();
        assert translatedCall instanceof JsInvocation;
        return (JsInvocation) translatedCall;
    }

    private static void setArguments(@NotNull TranslationContext context, @NotNull List<String> arguments,
            @NotNull JsInvocation translatedCall) {
        JsArrayLiteral arrayLiteral = new JsArrayLiteral();
        arrayLiteral.getExpressions().addAll(toStringLiteralList(arguments, context.program()));
        JsAstUtils.setArguments(translatedCall, Collections.<JsExpression>singletonList(arrayLiteral));
    }
}
