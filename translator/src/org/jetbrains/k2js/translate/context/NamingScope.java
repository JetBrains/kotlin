package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pavel Talanov
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
        JsName declaredName = scope.declareName(mayBeObfuscateName(name));
        descriptorToNameMap.put(descriptor, declaredName);
        return declaredName;
    }

    //TODO: temporary solution
    @NotNull
    private String mayBeObfuscateName(@NotNull String name) {
        int obfuscate = 0;
        String result = name;
//        while (scope.findExistingName(result) != null) {
//            result = name + "_" + obfuscate;
//            obfuscate++;
//        }
        return result;
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

    @NotNull
    public JsScope jsScope() {
        return scope;
    }
}
