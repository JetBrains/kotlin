package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
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
    public static Declarations newInstance(@NotNull NamingScope rootScope) {
        return new Declarations(rootScope);
    }

    @NotNull
    private final Map<DeclarationDescriptor, NamingScope> descriptorToScopeMap = new HashMap<DeclarationDescriptor, NamingScope>();
    @NotNull
    private final Map<DeclarationDescriptor, JsName> descriptorToNameMap = new HashMap<DeclarationDescriptor, JsName>();
    @NotNull
    private final Map<DeclarationDescriptor, JsNameRef> descriptorToQualifierMap = new HashMap<DeclarationDescriptor, JsNameRef>();
    @NotNull
    private final NamingScope rootScope;

    private Declarations(@NotNull NamingScope scope) {
        this.rootScope = scope;
    }

    public void extractDeclarations(@NotNull DeclarationDescriptor descriptor) {
        DeclarationVisitor visitor = new DeclarationVisitor(this);
        descriptor.accept(visitor, DeclarationContext.rootContext(rootScope, null));
    }

    public void extractStandardLibrary(@NotNull JetStandardLibrary standardLibrary,
                                       @NotNull JsNameRef standardLibraryObjectName) {
        DeclarationVisitor visitor = new DeclarationVisitor(this);
        for (DeclarationDescriptor descriptor :
                standardLibrary.getLibraryScope().getAllDescriptors()) {
            descriptor.accept(visitor, DeclarationContext.rootContext(rootScope,
                    standardLibraryObjectName));
        }
    }

    @NotNull
        /*package*/ NamingScope getScope(@NotNull DeclarationDescriptor descriptor) {
        NamingScope scope = descriptorToScopeMap.get(descriptor.getOriginal());
        assert scope != null : "Unknown declaration";
        return scope;
    }

    @NotNull
    public JsName getName(@NotNull DeclarationDescriptor descriptor) {
        JsName name = descriptorToNameMap.get(descriptor.getOriginal());
        assert name != null : "Unknown declaration";
        return name;
    }

    public boolean hasDeclaredName(@NotNull DeclarationDescriptor descriptor) {
        return descriptorToNameMap.containsKey(descriptor.getOriginal());
    }

    /*package*/ boolean hasQualifier(@NotNull DeclarationDescriptor descriptor) {
        return (descriptorToQualifierMap.get(descriptor.getOriginal()) != null);
    }

    @NotNull
        /*package*/ JsNameRef getQualifier(@NotNull DeclarationDescriptor descriptor) {
        JsNameRef qualifier = descriptorToQualifierMap.get(descriptor.getOriginal());
        assert qualifier != null : "Cannot be null. Use hasQualifier to check.";
        return qualifier;
    }

    /*package*/ void putScope(@NotNull DeclarationDescriptor descriptor, @NotNull NamingScope scope) {
//        assert !descriptorToScopeMap.containsKey(descriptor) : "Already contains that key!";
        descriptorToScopeMap.put(descriptor, scope);
    }

    //TODO: decide what to do about function overriding
    /*package*/ void putName(@NotNull DeclarationDescriptor descriptor, @NotNull JsName name) {
//        assert !descriptorToNameMap.containsKey(descriptor) : "Already contains that key!";
        descriptorToNameMap.put(descriptor, name);
    }

    /*package*/ void putQualifier(@NotNull DeclarationDescriptor descriptor, @Nullable JsNameRef qualifier) {
//        assert !descriptorToQualifierMap.containsKey(descriptor) : "Already contains that key!";
        descriptorToQualifierMap.put(descriptor, qualifier);
    }
}
