package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsBlock;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

public class DynamicContext {

    @NotNull
    public static DynamicContext rootContext(@NotNull NamingScope rootScope, @NotNull JsBlock globalBlock) {
        return new DynamicContext(rootScope, globalBlock);
    }

    @NotNull
    public static DynamicContext contextWithScope(@NotNull NamingScope scope) {
        return new DynamicContext(scope, new JsBlock());
    }

    @NotNull
    private NamingScope namingScope;
    @NotNull
    private JsBlock currentBlock;

    private DynamicContext(@NotNull NamingScope scope, @NotNull JsBlock block) {
        this.namingScope = scope;
        this.currentBlock = block;
    }

    @NotNull
    public DynamicContext innerScope(@NotNull JsScope scope) {
        return new DynamicContext(namingScope.innerScope(scope), currentBlock);
    }

    @NotNull
    public DynamicContext innerBlock(@NotNull JsBlock block) {
        return new DynamicContext(namingScope, block);
    }

    @NotNull
    public JsName getLocalName(@NotNull DeclarationDescriptor descriptor) {
        JsName name = namingScope.getName(descriptor);
        assert name != null : descriptor.getName() + " is not declared. Use isDeclared to check.";
        return name;
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return namingScope.isDeclared(descriptor);
    }

    @NotNull
    public TemporaryVariable declareTemporary(@NotNull JsExpression initExpression) {
        return new TemporaryVariable(namingScope.declareTemporary(), initExpression);
    }

    @NotNull
    public TemporaryVariable declareTemporaryWithName(@NotNull String preferredName,
                                                      @NotNull JsExpression initExpression) {
        return new TemporaryVariable(namingScope.declareTemporaryWithName(preferredName), initExpression);
    }

    @NotNull
    public JsName declareLocalVariable(@NotNull DeclarationDescriptor descriptor) {
        return namingScope.declareVariable(descriptor, descriptor.getName());
    }

    @NotNull
    public JsScope jsScope() {
        return namingScope.jsScope();
    }

    @NotNull
    public JsBlock jsBlock() {
        return currentBlock;
    }
}