/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline;

import com.intellij.psi.PsiElement;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CommonCoroutineCodegenUtilKt;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticSink;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.inline.clean.*;
import org.jetbrains.kotlin.js.inline.context.FunctionContext;
import org.jetbrains.kotlin.js.inline.context.InliningContext;
import org.jetbrains.kotlin.js.inline.context.NamingContext;
import org.jetbrains.kotlin.js.inline.util.*;
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata;
import org.jetbrains.kotlin.resolve.inline.InlineStrategy;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jetbrains.kotlin.js.inline.util.CollectUtilsKt.getImportTag;
import static org.jetbrains.kotlin.js.inline.util.CollectionUtilsKt.IdentitySet;
import static org.jetbrains.kotlin.js.translate.declaration.InlineCoroutineUtilKt.transformSpecialFunctionsToCoroutineMetadata;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.flattenStatement;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn;

public class JsInliner extends JsVisitorWithContextImpl {

    private final JsConfig config;
    private final Map<JsName, FunctionWithWrapper> functions;
    private final Set<JsFunction> namedFunctionsSet;
    private final Map<String, FunctionWithWrapper> accessors;
    private final Stack<JsInliningContext> inliningContexts = new Stack<>();
    private final Set<JsFunction> processedFunctions = CollectionUtilsKt.IdentitySet();
    private final Set<JsFunction> inProcessFunctions = CollectionUtilsKt.IdentitySet();
    private final FunctionReader functionReader;
    private final DiagnosticSink trace;
    private Map<String, JsName> existingImports = new HashMap<>();
    private JsContext<JsStatement> statementContextForInline;
    private final Map<JsBlock, FunctionWithWrapper> functionsByWrapperNodes = new HashMap<>();
    private final Map<JsFunction, FunctionWithWrapper> functionsByFunctionNodes = new HashMap<>();

    // these are needed for error reporting, when inliner detects cycle
    private final Stack<JsFunction> namedFunctionsStack = new Stack<>();
    private final LinkedList<JsCallInfo> inlineCallInfos = new LinkedList<>();
    private final Function1<JsNode, Boolean> canBeExtractedByInliner =
            node -> node instanceof JsInvocation && hasToBeInlined((JsInvocation) node);
    private int inlineFunctionDepth;

    private final Map<JsWrapperKey, Map<JsName, JsNameRef>> replacementsInducedByWrappers = new HashMap<>();

    private final Map<JsName, String> inverseNameBindings;

    private Map<JsName, String> existingNameBindings = new HashMap<>();

    private final List<JsNameBinding> additionalNameBindings = new ArrayList<>();

    public static void process(
            @NotNull JsConfig.Reporter reporter,
            @NotNull JsConfig config,
            @NotNull DiagnosticSink trace,
            @NotNull JsName currentModuleName,
            @NotNull List<JsProgramFragment> fragments,
            @NotNull List<JsProgramFragment> fragmentsToProcess,
            @NotNull List<JsStatement> importStatements
    ) {
        Map<JsName, FunctionWithWrapper> functions = CollectUtilsKt.collectNamedFunctionsAndWrappers(fragments);
        Map<String, FunctionWithWrapper> accessors = CollectUtilsKt.collectAccessors(fragments);
        Map<JsName, String> inverseNameBindings = CollectUtilsKt.collectNameBindings(fragments);

        DummyAccessorInvocationTransformer accessorInvocationTransformer = new DummyAccessorInvocationTransformer();
        for (JsProgramFragment fragment : fragmentsToProcess) {
            accessorInvocationTransformer.accept(fragment.getDeclarationBlock());
            accessorInvocationTransformer.accept(fragment.getInitializerBlock());
        }
        FunctionReader functionReader = new FunctionReader(reporter, config, currentModuleName, fragments);
        JsInliner inliner = new JsInliner(config, functions, accessors, inverseNameBindings, functionReader, trace);

        for (JsStatement statement : importStatements) {
            inliner.processImportStatement(statement);
        }

        for (JsProgramFragment fragment : fragmentsToProcess) {
            inliner.existingImports.clear();
            inliner.additionalNameBindings.clear();
            inliner.existingNameBindings = CollectUtilsKt.collectNameBindings(Collections.singletonList(fragment));

            inliner.acceptStatement(fragment.getDeclarationBlock());

            // There can be inlined function in top-level initializers, we need to optimize them as well
            JsFunction fakeInitFunction = new JsFunction(JsDynamicScope.INSTANCE, fragment.getInitializerBlock(), "");
            JsGlobalBlock initWrapper = new JsGlobalBlock();
            initWrapper.getStatements().add(new JsExpressionStatement(fakeInitFunction));
            inliner.accept(initWrapper);
            initWrapper.getStatements().remove(initWrapper.getStatements().size() - 1);

            fragment.getInitializerBlock().getStatements().addAll(0, initWrapper.getStatements());
            fragment.getNameBindings().addAll(inliner.additionalNameBindings);
        }

        for (JsProgramFragment fragment : fragmentsToProcess) {
            JsBlock block = new JsBlock(fragment.getDeclarationBlock(), fragment.getInitializerBlock(), fragment.getExportBlock());
            RemoveUnusedImportsKt.removeUnusedImports(block);
            SimplifyWrappedFunctionsKt.simplifyWrappedFunctions(block);
            RemoveUnusedFunctionDefinitionsKt.removeUnusedFunctionDefinitions(block, CollectUtilsKt.collectNamedFunctions(block));
        }
    }

    private JsInliner(
            @NotNull JsConfig config,
            @NotNull Map<JsName, FunctionWithWrapper> functions,
            @NotNull Map<String, FunctionWithWrapper> accessors,
            @NotNull Map<JsName, String> inverseNameBindings,
            @NotNull FunctionReader functionReader,
            @NotNull DiagnosticSink trace
    ) {
        this.config = config;
        this.functions = functions;
        this.namedFunctionsSet = IdentitySet();
        for (FunctionWithWrapper functionWithWrapper : functions.values()) {
            namedFunctionsSet.add(functionWithWrapper.getFunction());
        }
        this.accessors = accessors;
        this.inverseNameBindings = inverseNameBindings;
        this.functionReader = functionReader;
        this.trace = trace;

        Stream.concat(functions.values().stream(), accessors.values().stream())
                .forEach(f -> {
                    functionsByFunctionNodes.put(f.getFunction(), f);
                    if (f.getWrapperBody() != null) {
                        functionsByWrapperNodes.put(f.getWrapperBody(), f);
                    }
                });
    }

    private void processImportStatement(JsStatement statement) {
        if (statement instanceof JsVars) {
            JsVars jsVars = (JsVars) statement;
            String tag = getImportTag(jsVars);
            if (tag != null) {
                existingImports.put(tag, jsVars.getVars().get(0).getName());
            }
        }
    }

    @Override
    public boolean visit(@NotNull JsFunction function, @NotNull JsContext context) {
        FunctionWithWrapper functionWithWrapper = functionsByFunctionNodes.get(function);
        if (functionWithWrapper != null) {
            visit(functionWithWrapper);
            return false;
        }
        else {
            if (statementContextForInline == null) {
                statementContextForInline = getLastStatementLevelContext();
                startFunction(function);
                boolean result = super.visit(function, context);
                statementContextForInline = null;
                return result;
            } else {
                startFunction(function);
                return super.visit(function, context);
            }
        }
    }

    @Override
    public void endVisit(@NotNull JsFunction function, @NotNull JsContext context) {
        super.endVisit(function, context);
        if (!functionsByFunctionNodes.containsKey(function)) {
            endFunction(function);
        }
    }

    private void startFunction(@NotNull JsFunction function) {
        inliningContexts.push(new JsInliningContext(statementContextForInline));

        assert !inProcessFunctions.contains(function): "Inliner has revisited function";
        inProcessFunctions.add(function);

        if (namedFunctionsSet.contains(function)) {
            namedFunctionsStack.push(function);
        }
    }

    private void endFunction(@NotNull JsFunction function) {
        NamingUtilsKt.refreshLabelNames(function.getBody(), function.getScope());

        RemoveUnusedLocalFunctionDeclarationsKt.removeUnusedLocalFunctionDeclarations(function);
        processedFunctions.add(function);

        new FunctionPostProcessor(function).apply();

        assert inProcessFunctions.contains(function);
        inProcessFunctions.remove(function);

        inliningContexts.pop();

        if (!namedFunctionsStack.empty() && namedFunctionsStack.peek() == function) {
            namedFunctionsStack.pop();
        }
    }

    @Override
    public boolean visit(@NotNull JsBlock x, @NotNull JsContext ctx) {
        FunctionWithWrapper functionWithWrapper = functionsByWrapperNodes.get(x);
        if (functionWithWrapper != null) {
            visit(functionWithWrapper);
            return false;
        }
        return super.visit(x, ctx);
    }

    private void visit(@NotNull FunctionWithWrapper functionWithWrapper) {
        JsContext<JsStatement> oldContextForInline = statementContextForInline;
        Map<String, JsName> oldExistingImports = existingImports;
        int oldInlineFunctionDepth = inlineFunctionDepth;

        ListContext<JsStatement> innerContext = new ListContext<>();

        JsBlock wrapperBody = functionWithWrapper.getWrapperBody();
        List<JsStatement> statements = null;
        if (wrapperBody != null) {
            existingImports = new HashMap<>();
            statementContexts.push(innerContext);
            statementContextForInline = innerContext;
            inlineFunctionDepth++;

            for (JsStatement statement : wrapperBody.getStatements()) {
                processImportStatement(statement);
            }
            assert functionWithWrapper.getWrapperBody() != null;
            statements = functionWithWrapper.getWrapperBody().getStatements();
            if (!statements.isEmpty() && statements.get(statements.size() - 1) instanceof JsReturn) {
                statements = statements.subList(0, statements.size() - 1);
            }

            innerContext.traverse(statements);
            statementContexts.pop();
        } else {
            if (statementContextForInline == null) statementContextForInline = getLastStatementLevelContext();
        }

        startFunction(functionWithWrapper.getFunction());

        JsBlock block = new JsBlock(functionWithWrapper.getFunction().getBody());
        innerContext.traverse(block.getStatements());
        functionWithWrapper.getFunction().getBody().traverse(this, innerContext);

        endFunction(functionWithWrapper.getFunction());

        if (statements != null) {
            statements.addAll(block.getStatements().subList(0, block.getStatements().size() - 1));
        }

        statementContextForInline = oldContextForInline;
        existingImports = oldExistingImports;
        inlineFunctionDepth = oldInlineFunctionDepth;
    }

    @Override
    public boolean visit(@NotNull JsInvocation call, @NotNull JsContext context) {
        if (!hasToBeInlined(call)) return true;

        JsFunction containingFunction = getCurrentNamedFunction();

        if (containingFunction != null) {
            inlineCallInfos.add(new JsCallInfo(call, containingFunction));
        }

        FunctionWithWrapper definition = getFunctionContext().getFunctionDefinition(call);

        if (inProcessFunctions.contains(definition.getFunction()))  {
            reportInlineCycle(call, definition.getFunction());
        }
        else if (!processedFunctions.contains(definition.getFunction())) {
            for (int i = 0; i < call.getArguments().size(); ++i) {
                JsExpression argument = call.getArguments().get(i);
                call.getArguments().set(i, accept(argument));
            }
            visit(definition);
            return false;
        }

        return true;
    }

    @Override
    public void endVisit(@NotNull JsInvocation x, @NotNull JsContext ctx) {
        if (hasToBeInlined(x)) {
            inline(x, ctx);
        }

        JsCallInfo lastCallInfo = null;

        if (!inlineCallInfos.isEmpty()) {
            lastCallInfo = inlineCallInfos.getLast();
        }

        if (lastCallInfo != null && lastCallInfo.call == x) {
            inlineCallInfos.removeLast();
        }
    }

    @Override
    protected void doAcceptStatementList(List<JsStatement> statements) {
        // at top level of js ast, contexts stack can be empty,
        // but there is no inline calls anyway
        if(!inliningContexts.isEmpty()) {
            int i = 0;

            while (i < statements.size()) {
                List<JsStatement> additionalStatements =
                        ExpressionDecomposer.preserveEvaluationOrder(statements.get(i), canBeExtractedByInliner);
                statements.addAll(i, additionalStatements);
                i += additionalStatements.size() + 1;
            }
        }

        super.doAcceptStatementList(statements);
    }

    private void inline(@NotNull JsInvocation call, @NotNull JsContext context) {
        DeclarationDescriptor callDescriptor = MetadataProperties.getDescriptor(call);
        if (isSuspendWithCurrentContinuation(callDescriptor,
                                             CommonConfigurationKeysKt.getLanguageVersionSettings(config.getConfiguration()))) {
            inlineSuspendWithCurrentContinuation(call, context);
            return;
        }

        JsInliningContext inliningContext = getInliningContext();
        FunctionWithWrapper functionWithWrapper = inliningContext.getFunctionContext().getFunctionDefinition(call);

        // Since we could get functionWithWrapper as a simple function directly from staticRef (which always points on implementation)
        // we should check if we have a known wrapper for it
        if (functionsByFunctionNodes.containsKey(functionWithWrapper.getFunction())) {
            functionWithWrapper = functionsByFunctionNodes.get(functionWithWrapper.getFunction());
        }

        JsFunction function = functionWithWrapper.getFunction().deepCopy();
        function.setBody(transformSpecialFunctionsToCoroutineMetadata(function.getBody()));
        if (functionWithWrapper.getWrapperBody() != null) {
            applyWrapper(functionWithWrapper.getWrapperBody(), function, functionWithWrapper.getFunction(), inliningContext);
        }
        InlineableResult inlineableResult = FunctionInlineMutator.getInlineableCallReplacement(call, function, inliningContext);

        JsStatement inlineableBody = inlineableResult.getInlineableBody();
        JsExpression resultExpression = inlineableResult.getResultExpression();
        JsContext<JsStatement> statementContext = inliningContext.getStatementContext();
        // body of inline function can contain call to lambdas that need to be inlined
        JsStatement inlineableBodyWithLambdasInlined = accept(inlineableBody);
        assert inlineableBody == inlineableBodyWithLambdasInlined;

        // Support non-local return from secondary constructor
        // Returns from secondary constructors should return `$this` object.
        JsFunction currentFunction = getCurrentNamedFunction();
        if (currentFunction != null) {
            JsName returnVariable = MetadataProperties.getForcedReturnVariable(currentFunction);
            if (returnVariable != null) {
                inlineableBody.accept(new RecursiveJsVisitor() {
                    @Override
                    public void visitReturn(@NotNull JsReturn x) {
                        x.setExpression(returnVariable.makeRef());
                    }
                });
            }
        }

        statementContext.addPrevious(flattenStatement(inlineableBody));

        /*
         * Assumes, that resultExpression == null, when result is not needed.
         * @see FunctionInlineMutator.isResultNeeded()
         */
        if (resultExpression == null) {
            statementContext.removeMe();
            return;
        }

        resultExpression = accept(resultExpression);
        MetadataProperties.setSynthetic(resultExpression, true);
        context.replaceMe(resultExpression);
    }

    private void applyWrapper(
            @NotNull JsBlock wrapper, @NotNull JsFunction function, @NotNull JsFunction originalFunction,
            @NotNull InliningContext inliningContext
    ) {
        // Apparently we should avoid this trick when we implement fair support for crossinline
        Function<JsWrapperKey, Map<JsName, JsNameRef>> replacementGen = k -> {
            JsContext ctx = k.context;

            Map<JsName, JsNameRef> newReplacements = new HashMap<>();

            List<JsStatement> copiedStatements = new ArrayList<>();
            for (JsStatement statement : wrapper.getStatements()) {
                if (statement instanceof JsReturn) continue;

                statement = statement.deepCopy();
                if (inlineFunctionDepth == 0) {
                    replaceExpressionsWithLocalAliases(statement);
                }

                if (statement instanceof JsVars) {
                    JsVars jsVars = (JsVars) statement;
                    String tag = getImportTag(jsVars);
                    if (tag != null) {
                        JsName name = jsVars.getVars().get(0).getName();
                        JsName existingName = inlineFunctionDepth == 0 ? MetadataProperties.getLocalAlias(name) : null;
                        if (existingName == null) {
                            existingName = existingImports.computeIfAbsent(tag, t -> {
                                copiedStatements.add(jsVars);
                                JsName alias = JsScope.declareTemporaryName(name.getIdent());
                                alias.copyMetadataFrom(name);
                                newReplacements.put(name, pureFqn(alias, null));
                                return alias;
                            });
                        }

                        if (name != existingName) {
                            JsNameRef replacement = pureFqn(existingName, null);
                            newReplacements.put(name, replacement);
                        }

                        continue;
                    }
                }

                copiedStatements.add(statement);
            }

            Set<JsName> definedNames = copiedStatements.stream()
                    .flatMap(node -> CollectUtilsKt.collectDefinedNamesInAllScopes(node).stream())
                    .filter(name -> !newReplacements.containsKey(name))
                    .collect(Collectors.toSet());
            for (JsName name : definedNames) {
                JsName alias = JsScope.declareTemporaryName(name.getIdent());
                alias.copyMetadataFrom(name);
                JsNameRef replacement = pureFqn(alias, null);
                newReplacements.put(name, replacement);
            }

            for (JsStatement statement : copiedStatements) {
                statement = RewriteUtilsKt.replaceNames(statement, newReplacements);
                ctx.addPrevious(accept(statement));
            }

            for (Map.Entry<JsName, JsFunction> entry : CollectUtilsKt.collectNamedFunctions(new JsBlock(copiedStatements)).entrySet()) {
                if (MetadataProperties.getStaticRef(entry.getKey()) instanceof JsFunction) {
                    MetadataProperties.setStaticRef(entry.getKey(), entry.getValue());
                }
            }

            return newReplacements;
        };

        JsWrapperKey key = new JsWrapperKey(inliningContext.getStatementContextBeforeCurrentFunction(), originalFunction);
        Map<JsName, JsNameRef> replacements = replacementsInducedByWrappers.computeIfAbsent(key, replacementGen);

        RewriteUtilsKt.replaceNames(function, replacements);

        // Copy nameBinding's for inlined localAlias'es
        for (JsNameRef nameRef : replacements.values()) {
            JsName name = nameRef.getName();
            if (name != null && !existingNameBindings.containsKey(name)) {
                String tag = inverseNameBindings.get(name);
                if (tag != null) {
                    existingNameBindings.put(name, tag);
                    additionalNameBindings.add(new JsNameBinding(tag, name));
                }
            }
        }
    }

    private static void replaceExpressionsWithLocalAliases(@NotNull JsStatement statement) {
        new JsVisitorWithContextImpl() {
            @Override
            public void endVisit(@NotNull JsNameRef x, @NotNull JsContext ctx) {
                replaceIfNecessary(x, ctx);
            }

            @Override
            public void endVisit(@NotNull JsArrayAccess x, @NotNull JsContext ctx) {
                replaceIfNecessary(x, ctx);
            }

            private void replaceIfNecessary(@NotNull JsExpression expression, @NotNull JsContext context) {
                JsName alias = MetadataProperties.getLocalAlias(expression);
                if (alias != null) {
                    context.replaceMe(alias.makeRef());
                }
            }

        }.accept(statement);
    }

    private static boolean isSuspendWithCurrentContinuation(
            @Nullable DeclarationDescriptor descriptor,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        if (!(descriptor instanceof FunctionDescriptor)) return false;
        return CommonCoroutineCodegenUtilKt.isBuiltInSuspendCoroutineUninterceptedOrReturn(
                (FunctionDescriptor) descriptor.getOriginal(), languageVersionSettings
        );
    }

    private void inlineSuspendWithCurrentContinuation(@NotNull JsInvocation call, @NotNull JsContext context) {
        JsExpression lambda = call.getArguments().get(0);
        JsExpression continuationArg = call.getArguments().get(call.getArguments().size() - 1);

        JsInvocation invocation = new JsInvocation(lambda, continuationArg);
        MetadataProperties.setSuspend(invocation, true);
        context.replaceMe(accept(invocation));
    }

    @NotNull
    private JsInliningContext getInliningContext() {
        return inliningContexts.peek();
    }

    @NotNull
    private FunctionContext getFunctionContext() {
        return getInliningContext().getFunctionContext();
    }

    @Nullable
    private JsFunction getCurrentNamedFunction() {
        if (namedFunctionsStack.empty()) return null;
        return namedFunctionsStack.peek();
    }

    private void reportInlineCycle(@NotNull JsInvocation call, @NotNull JsFunction calledFunction) {
        MetadataProperties.setInlineStrategy(call, InlineStrategy.NOT_INLINE);
        Iterator<JsCallInfo> it = inlineCallInfos.descendingIterator();

        while (it.hasNext()) {
            JsCallInfo callInfo = it.next();
            PsiElement psiElement = MetadataProperties.getPsiElement(callInfo.call);

            CallableDescriptor descriptor = MetadataProperties.getDescriptor(callInfo.call);
            if (psiElement != null && descriptor != null) {
                trace.report(Errors.INLINE_CALL_CYCLE.on(psiElement, descriptor));
            }

            if (callInfo.containingFunction == calledFunction) {
                break;
            }
        }
    }

    private boolean hasToBeInlined(@NotNull JsInvocation call) {
        InlineStrategy strategy = MetadataProperties.getInlineStrategy(call);
        if (strategy == null || !strategy.isInline()) return false;

        return getFunctionContext().hasFunctionDefinition(call);
    }

    private class JsInliningContext implements InliningContext {
        private final FunctionContext functionContext;

        @NotNull
        private final JsContext<JsStatement> statementContextBeforeCurrentFunction;

        JsInliningContext(@NotNull JsContext<JsStatement> statementContextBeforeCurrentFunction) {
            functionContext = new FunctionContext(functionReader, config) {
                @Nullable
                @Override
                protected FunctionWithWrapper lookUpStaticFunction(@Nullable JsName functionName) {
                    return functions.get(functionName);
                }

                @Nullable
                @Override
                protected FunctionWithWrapper lookUpStaticFunctionByTag(@NotNull String functionTag) {
                    return accessors.get(functionTag);
                }
            };
            this.statementContextBeforeCurrentFunction = statementContextBeforeCurrentFunction;
        }

        @NotNull
        @Override
        public NamingContext newNamingContext() {
            return new NamingContext(getStatementContext());
        }

        @NotNull
        @Override
        public JsContext<JsStatement> getStatementContext() {
            return getLastStatementLevelContext();
        }

        @NotNull
        @Override
        public FunctionContext getFunctionContext() {
            return functionContext;
        }

        @NotNull
        @Override
        public JsContext<JsStatement> getStatementContextBeforeCurrentFunction() {
            return statementContextBeforeCurrentFunction;
        }
    }

    private static class JsCallInfo {
        @NotNull
        public final JsInvocation call;

        @NotNull
        public final JsFunction containingFunction;

        private JsCallInfo(@NotNull JsInvocation call, @NotNull JsFunction function) {
            this.call = call;
            containingFunction = function;
        }
    }

    static class JsWrapperKey {
        final JsContext context;
        private final JsFunction function;

        public JsWrapperKey(@NotNull JsContext context, @NotNull JsFunction function) {
            this.context = context;
            this.function = function;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsWrapperKey key = (JsWrapperKey) o;
            return Objects.equals(context, key.context) && Objects.equals(function, key.function);
        }

        @Override
        public int hashCode() {
            return Objects.hash(context, function);
        }
    }
}
