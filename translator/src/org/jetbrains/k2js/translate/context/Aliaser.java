package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

//TODO: implement aliasesForDescriptors stack for this need TESTS
public class Aliaser {

    public static Aliaser newInstance() {
        return new Aliaser();
    }

    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForDescriptors = new HashMap<DeclarationDescriptor, JsName>();
    @NotNull
    private Stack<JsName> thisAliases = new Stack<JsName>();

    private Aliaser() {
    }

    @NotNull
    public JsNameRef getAliasForThis() {
        assert !thisAliases.empty() : "No alias. Use hasAliasForThis function to check.";
        return thisAliases.peek().makeRef();
    }

    @SuppressWarnings("NullableProblems")
    public void setAliasForThis(@NotNull JsName alias) {
        thisAliases.push(alias);
    }

    //NOTE: here we are passing alias to check that they are set and removed in consistent order.
    public void removeAliasForThis(@NotNull JsName aliasToRemove) {
        assert !thisAliases.empty() : "Error: removing alias, when it is not set.";
        JsName lastAlias = thisAliases.pop();
        assert lastAlias.equals(aliasToRemove) : "Error: inconsistent alias removing.";
    }

    public boolean hasAliasForThis() {
        return (!thisAliases.empty());
    }

    public boolean hasAliasForDeclaration(@NotNull DeclarationDescriptor declaration) {
        return aliasesForDescriptors.containsKey(declaration.getOriginal());
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
}