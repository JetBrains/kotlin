package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class DeclarationExtractor {
    private final Map<DeclarationDescriptor, JsScope> descriptorToScopeMap
            = new HashMap<DeclarationDescriptor, JsScope>();

    public DeclarationExtractor() {

    }

    public void extractDeclarations(@NotNull DeclarationDescriptor descriptor, JsScope rootScope) {
        ExtractionVisitor visitor = new ExtractionVisitor(this);
        descriptor.accept(visitor, rootScope);
    }

    @NotNull
    public JsScope getScope(@NotNull DeclarationDescriptor descriptor) {
        JsScope scope = descriptorToScopeMap.get(descriptor);
        assert scope != null : "Unknown declaration";
        return scope;
    }

    /*package*/ void put(@NotNull DeclarationDescriptor descriptor, @NotNull JsScope scope) {
        descriptorToScopeMap.put(descriptor, scope);
    }
}
