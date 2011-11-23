package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class Declarations {
    private final Map<DeclarationDescriptor, JsScope> descriptorToScopeMap
            = new HashMap<DeclarationDescriptor, JsScope>();
    private final Map<DeclarationDescriptor, JsName> descriptorToNameMap
            = new HashMap<DeclarationDescriptor, JsName>();

    private Declarations() {

    }

    @NotNull
    static public Declarations extractDeclarations(@NotNull DeclarationDescriptor descriptor, JsScope rootScope) {
        Declarations declarations = new Declarations();
        ExtractionVisitor visitor = new ExtractionVisitor(declarations);
        descriptor.accept(visitor, rootScope);
        return declarations;
    }

    @NotNull
    public JsScope getScope(@NotNull DeclarationDescriptor descriptor) {
        JsScope scope = descriptorToScopeMap.get(descriptor);
        assert scope != null : "Unknown declaration";
        return scope;
    }

    @NotNull
    public JsName getName(@NotNull DeclarationDescriptor descriptor) {
        JsName name = descriptorToNameMap.get(descriptor);
        assert name != null : "Unknown declaration";
        return name;
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return descriptorToNameMap.containsKey(descriptor);
    }

    /*package*/ void putScope(@NotNull DeclarationDescriptor descriptor, @NotNull JsScope scope) {
        descriptorToScopeMap.put(descriptor, scope);
    }

    /*package*/ void putName(@NotNull DeclarationDescriptor descriptor, @NotNull JsName name) {
        descriptorToNameMap.put(descriptor, name);
    }
}
