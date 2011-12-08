package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.HashMap;
import java.util.Map;

//TODO: implement aliases stack for this
public class Aliaser {

    public static Aliaser aliasesForStandardClasses(@NotNull JetStandardLibrary standardLibrary,
                                                    @NotNull Namer namer) {
        Aliaser aliaser = new Aliaser();
        return aliaser;
    }

    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliases = new HashMap<DeclarationDescriptor, JsName>();
    @Nullable
    private JsName aliasForThis = null;

    private Aliaser() {
    }

    @NotNull
    public JsNameRef getAliasForThis() {
        assert aliasForThis != null : "Alias is null. Use hasAliasForThis function to check.";
        return aliasForThis.makeRef();
    }

    @SuppressWarnings("NullableProblems")
    public void setAliasForThis(@NotNull JsName alias) {
        aliasForThis = alias;
    }

    public void removeAliasForThis() {
        aliasForThis = null;
    }

    public boolean hasAliasForThis() {
        return (aliasForThis != null);
    }

    public boolean hasAliasForDeclaration(@NotNull DeclarationDescriptor declaration) {
        return aliases.containsKey(declaration.getOriginal());
    }

    @NotNull
    public JsName getAliasForDeclaration(@NotNull DeclarationDescriptor declaration) {
        JsName alias = aliases.get(declaration.getOriginal());
        assert alias != null : "Use has alias for declaration to check.";
        return alias;
    }

    public void setAliasForDescriptor(@NotNull DeclarationDescriptor declaration, @NotNull JsName alias) {
        assert (!hasAliasForDeclaration(declaration.getOriginal())) : "This declaration already has an alias!";
        aliases.put(declaration.getOriginal(), alias);
    }

    public void removeAliasForDescriptor(@NotNull DeclarationDescriptor declaration) {
        assert (hasAliasForDeclaration(declaration.getOriginal())) : "This declaration does not has an alias!";
        aliases.remove(declaration.getOriginal());
    }
}