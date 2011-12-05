package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class NamingScope {

    @NotNull
    public static NamingScope rootScope(@NotNull JsScope rootScope) {
        return new NamingScope(rootScope, null);
    }

    @NotNull
    private final JsScope scope;

    @Nullable
    private final NamingScope parent;

    @NotNull
    private final Map<DeclarationDescriptor, JsName> descriptorToNameMap =
            new HashMap<DeclarationDescriptor, JsName>();


    private NamingScope(@NotNull JsScope correspondingScope, @Nullable NamingScope parent) {
        this.scope = correspondingScope;
        this.parent = parent;
    }

    @NotNull
    public NamingScope innerScope(@NotNull String scopeName) {
        JsScope innerJsScope = new JsScope(jsScope(), scopeName);
        return new NamingScope(innerJsScope, this);
    }

    @NotNull
    public NamingScope innerScope(@NotNull JsScope correspondingScope) {
        return new NamingScope(correspondingScope, this);
    }

    @Nullable
    public JsName getName(@NotNull DeclarationDescriptor descriptor) {
        JsName name = descriptorToNameMap.get(descriptor);

        if (name != null) return name;
        if (parent != null) return parent.getName(descriptor);

        return null;
    }

    @NotNull
    public JsName declareVariable(@NotNull DeclarationDescriptor descriptor,
                                  @NotNull String name) {
        JsName declaredName = scope.declareName(name);
        descriptorToNameMap.put(descriptor, declaredName);
        return declaredName;
    }

    @NotNull
    public JsName declareVariable(@NotNull DeclarationDescriptor descriptor) {
        return declareVariable(descriptor, descriptor.getName());
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return (getName(descriptor.getOriginal()) != null);
    }

    //TODO protect global namespace
    public JsName declareTemporary() {
        return scope.declareTemporary();
    }

    public JsName declareTemporaryWithName(@NotNull String preferredName) {
        return scope.declareName(preferredName);
    }

    @NotNull
    public JsScope jsScope() {
        return scope;
    }
}
