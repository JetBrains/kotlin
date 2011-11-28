package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetDeclaration;

import java.util.HashMap;
import java.util.Map;

public class Aliaser {
    @NotNull
    private final Map<JetDeclaration, JsName> aliases = new HashMap<JetDeclaration, JsName>();
    @Nullable
    private JsName aliasForThis = null;

    public Aliaser() {
    }

    @NotNull
    public JsNameRef getAliasForThis() {
        assert aliasForThis != null : "Alias is null. Use hasAliasForThis function to check.";
        return aliasForThis.makeRef();
    }

    public void setAliasForThis(@NotNull JsName alias) {
        aliasForThis = alias;
    }

    public void removeAliasForThis() {
        aliasForThis = null;
    }

    public boolean hasAliasForThis() {
        return (aliasForThis != null);
    }

    public boolean hasAliasForDeclaration(@NotNull JetDeclaration declaration) {
        return aliases.containsKey(declaration);
    }

    @NotNull
    public JsNameRef getAliasForDeclaration(@NotNull JetDeclaration declaration) {
        JsName alias = aliases.get(declaration);
        assert alias != null : "Use has alias for declaration to check.";
        return alias.makeRef();
    }

    public void setAliasForDeclaration(@NotNull JetDeclaration declaration, @NotNull JsName alias) {
        assert (!hasAliasForDeclaration(declaration)) : "This declaration already has an alias!";
        aliases.put(declaration, alias);
    }

    public void removeAliasForDeclaration(@NotNull JetDeclaration declaration) {
        assert (hasAliasForDeclaration(declaration)) : "This declaration does not has an alias!";
        aliases.remove(declaration);
    }
}