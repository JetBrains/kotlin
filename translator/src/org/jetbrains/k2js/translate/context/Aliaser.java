package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Aliaser {

    public static Aliaser newInstance() {
        return new Aliaser();
    }

    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForDescriptors = new HashMap<DeclarationDescriptor, JsName>();
    @NotNull
    private final Map<DeclarationDescriptor, Stack<JsName>> aliasesForThis
            = new HashMap<DeclarationDescriptor, Stack<JsName>>();

    private Aliaser() {
    }

    @NotNull
    public JsNameRef getAliasForThis(@NotNull DeclarationDescriptor descriptor) {
        Stack<JsName> aliasStack = aliasesForThis.get(descriptor.getOriginal());
        assert !aliasStack.empty();
        JsName aliasName = aliasStack.peek();
        assert aliasName != null : "This " + descriptor.getOriginal() + " doesn't have an alias.";
        return aliasName.makeRef();
    }

    public void setAliasForThis(@NotNull DeclarationDescriptor descriptor, @NotNull JsName alias) {
        Stack<JsName> aliasStack = aliasesForThis.get(descriptor.getOriginal());
        if (aliasStack == null) {
            aliasStack = new Stack<JsName>();
            aliasesForThis.put(descriptor, aliasStack);
        }
        aliasStack.push(alias);
    }

    public void removeAliasForThis(@NotNull DeclarationDescriptor descriptor) {
        Stack<JsName> aliasStack = aliasesForThis.get(descriptor.getOriginal());
        assert !aliasStack.empty();
        aliasStack.pop();
        if (aliasStack.empty()) {
            aliasesForThis.put(descriptor, null);
        }
    }

    public boolean hasAliasForThis(@NotNull DeclarationDescriptor descriptor) {
        Stack<JsName> aliasStack = aliasesForThis.get(descriptor.getOriginal());
        if (aliasStack == null) {
            return false;
        }
        return (!aliasStack.empty());
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