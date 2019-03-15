/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.TranslationUnit;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationRuntimeException;
import org.jetbrains.kotlin.js.facade.exceptions.UnsupportedFeatureException;
import org.jetbrains.kotlin.js.naming.NameSuggestion;
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver;
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator;
import org.jetbrains.kotlin.js.translate.context.StaticContext;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.declaration.FileDeclarationVisitor;
import org.jetbrains.kotlin.js.translate.expression.ExpressionVisitor;
import org.jetbrains.kotlin.js.translate.expression.PatternTranslator;
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator;
import org.jetbrains.kotlin.js.translate.test.JSTestGenerator;
import org.jetbrains.kotlin.js.translate.utils.*;
import org.jetbrains.kotlin.js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.protobuf.CodedInputStream;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.IOException;
import java.util.*;

import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.convertToStatement;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.toStringLiteralList;
import static org.jetbrains.kotlin.js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;

/**
 * This class provides a interface which all translators use to interact with each other.
 * Goal is to simplify interaction between translators.
 */
public final class Translation {
    private static final String ENUM_SIGNATURE = "kotlin$Enum";

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
        if (compileTimeValue != null && !compileTimeValue.getUsesNonConstValAsConstant()) {
            KotlinType type = context.bindingContext().getType(expression);
            if (type != null && (KotlinBuiltIns.isLong(type) || KotlinBuiltIns.isInt(type))) {
                JsExpression constantResult = translateConstant(compileTimeValue, expression, context);
                if (constantResult != null) {
                    constantResult.setSource(expression);

                    if (KotlinBuiltIns.isLong(type)) {
                        KtSimpleNameExpression referenceExpression = PsiUtils.getSimpleName(expression);
                        if (referenceExpression != null) {
                            DeclarationDescriptor descriptor =
                                    BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), referenceExpression);
                            if (descriptor != null) {
                                return context.declareConstantValue(descriptor, referenceExpression, constantResult);
                            }
                        }

                        String name = NameSuggestion.sanitizeName("L" + compileTimeValue.getValue(type).toString());
                        return context.declareConstantValue(name, "constant:" + name, constantResult, null);
                    }

                    if (KotlinBuiltIns.isInt(type)) {
                        return constantResult;
                    }
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
        JsExpression result = translateConstantWithoutType(constant, context);
        if (result != null) {
            MetadataProperties.setType(result, expectedType);
        }
        return result;
    }

    @Nullable
    private static JsExpression translateConstantWithoutType(@NotNull ConstantValue<?> constant, @NotNull TranslationContext context) {
        if (constant instanceof NullValue) {
            return new JsNullLiteral();
        }
        if (constant instanceof UnsignedValueConstant<?>) {
            return translateUnsignedConstant((UnsignedValueConstant<?>) constant, context);
        }

        Object value = constant.getValue();
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return new JsIntLiteral(((Number) value).intValue());
        }
        else if (value instanceof Long) {
            return JsAstUtils.newLong((Long) value);
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
            return new JsDoubleLiteral(doubleValue);
        }
        else if (value instanceof Number) {
            return new JsDoubleLiteral(((Number) value).doubleValue());
        }
        else if (value instanceof Boolean) {
            return new JsBooleanLiteral((Boolean) value);
        }

        //TODO: test
        if (value instanceof String) {
            return new JsStringLiteral((String) value);
        }
        if (value instanceof Character) {
            return new JsIntLiteral(((Character) value).charValue());
        }

        return null;
    }

    @Nullable
    private static JsExpression translateUnsignedConstant(
            @NotNull UnsignedValueConstant<?> unsignedConstant,
            @NotNull TranslationContext context
    ) {
        if (unsignedConstant instanceof UByteValue) {
            return JsAstUtils.byteToUByte(((UByteValue) unsignedConstant).getValue(), context);
        }
        else if (unsignedConstant instanceof UShortValue) {
            return JsAstUtils.shortToUShort(((UShortValue) unsignedConstant).getValue(), context);
        }
        else if (unsignedConstant instanceof UIntValue) {
            return JsAstUtils.intToUInt(((UIntValue) unsignedConstant).getValue(), context);
        }
        else if (unsignedConstant instanceof ULongValue) {
            Long value = ((ULongValue) unsignedConstant).getValue();
            JsExpression longExpression = JsAstUtils.newLong(value);
            return JsAstUtils.longToULong(longExpression, context);
        } else {
            return null;
        }
    }

    @NotNull
    private static JsNode doTranslateExpression(KtExpression expression, TranslationContext context) {
        try {
            return expression.accept(new ExpressionVisitor(), context);
        }
        catch (TranslationRuntimeException e) {
            throw e;
        }
        catch (RuntimeException | AssertionError e) {
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

        if (jsNode instanceof JsExpression) {
            JsExpression jsExpression = (JsExpression) jsNode;
            KotlinType type = context.bindingContext().getType(expression);
            if (MetadataProperties.getType(jsExpression) == null) {
                MetadataProperties.setType(jsExpression, type);
            }
            else if (type != null) {
                jsExpression = TranslationUtils.coerce(context, jsExpression, type);
            }
            return jsExpression;
        }

        assert jsNode instanceof JsStatement : "Unexpected node of type: " + jsNode.getClass().toString();
        if (BindingContextUtilsKt.isUsedAsExpression(expression, context.bindingContext())) {
            TemporaryVariable result = context.declareTemporary(null, expression);
            AssignToExpressionMutator saveResultToTemporaryMutator = new AssignToExpressionMutator(result.reference());
            block.getStatements().add(mutateLastExpression(jsNode, saveResultToTemporaryMutator));
            JsExpression tmpVar = result.reference();
            MetadataProperties.setType(tmpVar, context.bindingContext().getType(expression));
            return tmpVar;
        }

        block.getStatements().add(convertToStatement(jsNode));
        return new JsNullLiteral().source(expression);
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
    public static AstGenerationResult generateAst(
            @NotNull BindingTrace bindingTrace,
            @NotNull Collection<TranslationUnit> units,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull JsConfig config,
            @NotNull SourceFilePathResolver sourceFilePathResolver
    ) throws TranslationException {
        try {
            return doGenerateAst(bindingTrace, units, mainCallParameters, moduleDescriptor, config, sourceFilePathResolver);
        }
        catch (UnsupportedOperationException e) {
            throw new UnsupportedFeatureException("Unsupported feature used.", e);
        }
        catch (Throwable e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    private static AstGenerationResult doGenerateAst(
            @NotNull BindingTrace bindingTrace,
            @NotNull Collection<TranslationUnit> units,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull JsConfig config,
            @NotNull SourceFilePathResolver sourceFilePathResolver
    ) throws IOException {

        Map<String, TranslationUnit> inlineFunctionTagMap = new HashMap<>();

        Map<TranslationUnit.SourceFile, SourceFileTranslationResult> translatedSourceFiles = new LinkedHashMap<>();

        for (TranslationUnit unit : units) {
            if (unit instanceof TranslationUnit.SourceFile) {
                TranslationUnit.SourceFile sourceFileUnit = ((TranslationUnit.SourceFile) unit);
                KtFile file = sourceFileUnit.getFile();
                StaticContext staticContext = new StaticContext(bindingTrace, config, moduleDescriptor, sourceFilePathResolver, file.getPackageFqName().asString());
                TranslationContext context = TranslationContext.rootContext(staticContext);
                List<DeclarationDescriptor> fileMemberScope = new ArrayList<>();
                translateFile(context, file, fileMemberScope);

                JsProgramFragment fragment = staticContext.getFragment();
                for (String tag : staticContext.getInlineFunctionTags()) {
                    assert !inlineFunctionTagMap.containsKey(tag) : "Duplicate inline function tag found: '" + tag + "'";
                    inlineFunctionTagMap.put(tag, unit);
                }
                NormalizeImportTagsKt.normalizeImportTags(fragment);

                fragment.setTests(mayBeGenerateTests(context, file, fileMemberScope));
                fragment.setMainFunction(maybeGenerateCallToMain(context, config, moduleDescriptor, fileMemberScope, mainCallParameters));
                translatedSourceFiles.put(sourceFileUnit, new SourceFileTranslationResult(fragment, staticContext.getInlineFunctionTags(), fileMemberScope));
            }
            else if (unit instanceof TranslationUnit.BinaryAst) {
                byte[] inlineDataArray = ((TranslationUnit.BinaryAst) unit).getInlineData();
                JsAstProtoBuf.InlineData h = JsAstProtoBuf.InlineData.parseFrom(CodedInputStream.newInstance(inlineDataArray));
                for (String tag : h.getInlineFunctionTagsList()) {
                    assert !inlineFunctionTagMap.containsKey(tag) : "Duplicate inline function tag found: '" + tag + "'";
                    inlineFunctionTagMap.put(tag, unit);
                }
            }
        }
        return new AstGenerationResult(units,
                                       translatedSourceFiles,
                                       inlineFunctionTagMap,
                                       moduleDescriptor,
                                       config);
    }

    private static void translateFile(
            @NotNull TranslationContext context,
            @NotNull KtFile file,
            @NotNull List<DeclarationDescriptor> fileMemberScope
    ) {
        FileDeclarationVisitor fileVisitor = new FileDeclarationVisitor(context);

        try {
            for (KtDeclaration declaration : file.getDeclarations()) {
                DeclarationDescriptor descriptor = BindingUtils.getDescriptorForElement(context.bindingContext(), declaration);
                fileMemberScope.add(descriptor);
                if (!AnnotationsUtils.isPredefinedObject(descriptor)) {
                    declaration.accept(fileVisitor, context);
                }
            }
        }
        catch (TranslationRuntimeException e) {
            throw e;
        }
        catch (RuntimeException | AssertionError e) {
            throw new TranslationRuntimeException(file, e);
        }
    }

    @Nullable
    private static JsStatement mayBeGenerateTests(
            @NotNull TranslationContext context,
            @NotNull KtFile file,
            @NotNull List<DeclarationDescriptor> fileMemberScope
    ) {
        return new JSTestGenerator(context).generateTestCalls(file, fileMemberScope);
    }

    //TODO: determine whether should throw exception
    @Nullable
    private static JsStatement maybeGenerateCallToMain(
            @NotNull TranslationContext context,
            @NotNull JsConfig config,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull List<DeclarationDescriptor> fileMemberScope,
            @NotNull MainCallParameters mainCallParameters
    ) {
        if (!mainCallParameters.shouldBeGenerated()) return null;

        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(context.bindingContext(), config.getLanguageVersionSettings());

        FunctionDescriptor functionDescriptor = null;

        for (DeclarationDescriptor d : fileMemberScope) {
            if (mainFunctionDetector.isMain(d)) {
                functionDescriptor = (FunctionDescriptor)d;
            }
        }

        if (functionDescriptor == null) {
            return null;
        }

        int parameterCount = functionDescriptor.getValueParameters().size();
        assert parameterCount <= 1;

        List<JsExpression> args = new ArrayList<>();
        if (parameterCount != 0) {
            args.add(new JsArrayLiteral(toStringLiteralList(mainCallParameters.arguments())));
        }

        if (functionDescriptor.isSuspend()) {
            MemberScope scope = moduleDescriptor.getPackage(new FqNameUnsafe("kotlin.coroutines.js.internal").toSafe()).getMemberScope();
            PropertyDescriptor emptyContinuation = DescriptorUtils.getPropertyByName(scope, Name.identifier("EmptyContinuation"));

            args.add(ReferenceTranslator.translateAsValueReference(emptyContinuation, context));
            args.add(new JsBooleanLiteral(false));
        }

        JsExpression call = CallTranslator.INSTANCE.buildCall(context, functionDescriptor, args, null);
        return call.makeStmt();
    }
}
