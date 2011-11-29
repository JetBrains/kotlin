package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class Declarations {
    @NotNull
    private final Map<DeclarationDescriptor, JsScope> descriptorToScopeMap = new HashMap<DeclarationDescriptor, JsScope>();
    @NotNull
    private final Map<DeclarationDescriptor, JsName> descriptorToNameMap = new HashMap<DeclarationDescriptor, JsName>();
    @NotNull
    private final Map<DeclarationDescriptor, JsNameRef> descriptorToQualifierMap = new HashMap<DeclarationDescriptor, JsNameRef>();
    @NotNull
    private final JsScope rootScope;

    private Declarations(@NotNull JsScope scope) {
        this.rootScope = scope;
    }

    @NotNull
    static public Declarations newInstance(@NotNull JsScope rootScope) {
        return new Declarations(rootScope);
    }

    public void extractDeclarations(@NotNull DeclarationDescriptor descriptor) {
        DeclarationVisitor visitor = new DeclarationVisitor(this);
        descriptor.accept(visitor, DeclarationContext.rootContext(rootScope, null));
    }

    //TODO: provide a mechanism to do intrinsics
    public void extractStandardLibrary(@NotNull JetStandardLibrary standardLibrary) {
        DeclarationVisitor visitor = new DeclarationVisitor(this);
//        for (DeclarationDescriptor descriptor :
//                standardLibrary.getLibraryScope().getAllDescriptors()) {
//            descriptor.accept(visitor, rootScope);
//        }
        // standardLibrary.getArray().accept(visitor, rootScope);
    }

    @NotNull
    public JsScope getScope(@NotNull DeclarationDescriptor descriptor) {
        JsScope scope = descriptorToScopeMap.get(descriptor.getOriginal());
        assert scope != null : "Unknown declaration";
        return scope;
    }

    @NotNull
    public JsName getName(@NotNull DeclarationDescriptor descriptor) {
        JsName name = descriptorToNameMap.get(descriptor.getOriginal());
        assert name != null : "Unknown declaration";
        return name;
    }

    public boolean isDeclared(@NotNull DeclarationDescriptor descriptor) {
        return descriptorToNameMap.containsKey(descriptor.getOriginal());
    }

    /*package*/ void putScope(@NotNull DeclarationDescriptor descriptor, @NotNull JsScope scope) {
        assert !descriptorToScopeMap.containsKey(descriptor) : "Already contains that key!";
        descriptorToScopeMap.put(descriptor, scope);
    }

    /*package*/ void putName(@NotNull DeclarationDescriptor descriptor, @NotNull JsName name) {
        assert !descriptorToNameMap.containsKey(descriptor) : "Already contains that key!";
        descriptorToNameMap.put(descriptor, name);
    }

    /*package*/ void putQualifier(@NotNull DeclarationDescriptor descriptor, @Nullable JsNameRef qualifier) {
        assert !descriptorToQualifierMap.containsKey(descriptor) : "Already contains that key!";
        descriptorToQualifierMap.put(descriptor, qualifier);
    }
}
