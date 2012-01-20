package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;

//TODO: code gets duplicated
public class Aliaser {

    public static Aliaser newInstance() {
        return new Aliaser();
    }

    //TODO: rename
    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForDescriptors = new HashMap<DeclarationDescriptor, JsName>();
    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForThis = new HashMap<DeclarationDescriptor, JsName>();
    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForReceiver = new HashMap<DeclarationDescriptor, JsName>();

    private Aliaser() {
    }

    @NotNull
    public JsNameRef getAliasForThis(@NotNull DeclarationDescriptor descriptor) {
        JsName aliasName = aliasesForThis.get(descriptor.getOriginal());
        assert aliasName != null : "This " + descriptor.getOriginal() + " doesn't have an alias.";
        return aliasName.makeRef();
    }

    public void setAliasForThis(@NotNull DeclarationDescriptor descriptor, @NotNull JsName alias) {
        aliasesForThis.put(descriptor.getOriginal(), alias);
    }

    public void removeAliasForThis(@NotNull DeclarationDescriptor descriptor) {
        JsName removed = aliasesForThis.remove(descriptor.getOriginal());
        assert removed != null;
    }

    public boolean hasAliasForThis(@NotNull DeclarationDescriptor descriptor) {
        return (aliasesForThis.containsKey(descriptor.getOriginal()));
    }

    @NotNull
    public JsNameRef getAliasForReceiver(@NotNull DeclarationDescriptor descriptor) {
        JsName aliasName = aliasesForReceiver.get(descriptor.getOriginal());
        assert aliasName != null : "This descriptor doesn't have an alias.";
        return aliasName.makeRef();
    }

    public void setAliasForReceiver(@NotNull DeclarationDescriptor descriptor, @NotNull JsName alias) {
        aliasesForReceiver.put(descriptor.getOriginal(), alias);
    }

    public void removeAliasForReceiver(@NotNull DeclarationDescriptor descriptor) {
        JsName removed = aliasesForReceiver.remove(descriptor.getOriginal());
        assert removed != null;
    }

    public boolean hasAliasForReceiver(@NotNull DeclarationDescriptor descriptor) {
        return (aliasesForReceiver.containsKey(descriptor.getOriginal()));
    }


    @NotNull
    public JsName getAliasForDeclaration(@NotNull DeclarationDescriptor declaration) {
        JsName alias = aliasesForDescriptors.get(declaration.getOriginal());
        assert alias != null : "Use has alias for declaration to check.";
        return alias;
    }

    public void setAliasForDescriptor(@NotNull DeclarationDescriptor declaration, @NotNull JsName alias) {
        assert (!hasAliasForDeclaration(declaration.getOriginal())) : "This declaration already has an alias!";
        aliasesForDescriptors.put(declaration.getOriginal(), alias);
    }

    public void removeAliasForDescriptor(@NotNull DeclarationDescriptor declaration) {
        assert (hasAliasForDeclaration(declaration.getOriginal())) : "This declaration does not has an alias!";
        aliasesForDescriptors.remove(declaration.getOriginal());
    }

    public boolean hasAliasForDeclaration(@NotNull DeclarationDescriptor declaration) {
        return aliasesForDescriptors.containsKey(declaration.getOriginal());
    }


}