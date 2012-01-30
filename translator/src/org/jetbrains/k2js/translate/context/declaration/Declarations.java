package org.jetbrains.k2js.translate.context.declaration;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.k2js.translate.context.NamingScope;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pavel Talanov
 */
public final class Declarations {

    @NotNull
    /*package*/ static Declarations newInstance() {
        return new Declarations();
    }

    @NotNull
    private final Map<DeclarationDescriptor, NamingScope> descriptorToScopeMap = new HashMap<DeclarationDescriptor, NamingScope>();
    @NotNull
    private final Map<DeclarationDescriptor, JsName> descriptorToNameMap = new HashMap<DeclarationDescriptor, JsName>();
    @NotNull
    private final Map<DeclarationDescriptor, JsNameRef> descriptorToQualifierMap = new HashMap<DeclarationDescriptor, JsNameRef>();

    private Declarations() {
    }

    @NotNull
    public NamingScope getScope(@NotNull DeclarationDescriptor descriptor) {
        NamingScope scope = descriptorToScopeMap.get(descriptor.getOriginal());
        assert scope != null : "Unknown declaration";
        return scope;
    }

    @NotNull
    public JsName getName(@NotNull DeclarationDescriptor descriptor) {
        JsName name = descriptorToNameMap.get(descriptor.getOriginal());
        assert name != null : "Unknown declaration: " + DescriptorUtils.getFQName(descriptor);
        return name;
    }

    public boolean hasDeclaredName(@NotNull DeclarationDescriptor descriptor) {
        return descriptorToNameMap.containsKey(descriptor.getOriginal());
    }

    public boolean hasQualifier(@NotNull DeclarationDescriptor descriptor) {
        return (descriptorToQualifierMap.get(descriptor.getOriginal()) != null);
    }

    @NotNull
    public JsNameRef getQualifier(@NotNull DeclarationDescriptor descriptor) {
        JsNameRef qualifier = descriptorToQualifierMap.get(descriptor.getOriginal());
        assert qualifier != null : "Cannot be null. Use hasQualifier to check.";
        return qualifier;
    }

    /*package*/ void putScope(@NotNull DeclarationDescriptor descriptor, @NotNull NamingScope scope) {
        assert !descriptorToScopeMap.containsKey(descriptor)
                : "Already contains that key!\n" + descriptor;
        descriptorToScopeMap.put(descriptor, scope);
    }

    /*package*/ void putName(@NotNull DeclarationDescriptor descriptor, @NotNull JsName name) {
        assert !descriptorToNameMap.containsKey(descriptor)
                : "Already contains that key!\n" + descriptor;
        descriptorToNameMap.put(descriptor, name);
    }

    /*package*/ void putQualifier(@NotNull DeclarationDescriptor descriptor, @Nullable JsNameRef qualifier) {
        assert !descriptorToQualifierMap.containsKey(descriptor)
                : "Already contains that key!";
        descriptorToQualifierMap.put(descriptor, qualifier);
    }
}
