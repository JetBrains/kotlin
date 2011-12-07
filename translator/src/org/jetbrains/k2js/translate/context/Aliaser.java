package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getFunctionByName;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getPropertyByName;

//TODO: implement aliases stack for this
public class Aliaser {

    public static Aliaser aliasesForStandardClasses(@NotNull JetStandardLibrary standardLibrary,
                                                    @NotNull Namer namer) {
        Aliaser aliaser = new Aliaser();
        setAliasesForArray(standardLibrary, namer, aliaser);
        ClassDescriptor iteratorClass = (ClassDescriptor)
                standardLibrary.getLibraryScope().getClassifier("Iterator");
        setAliasesForIterator(namer, aliaser, iteratorClass);
        return aliaser;
    }

    private static void setAliasesForIterator(Namer namer, Aliaser aliaser, ClassDescriptor iteratorClass) {
        FunctionDescriptor nextFunction = getFunctionByName(iteratorClass.getDefaultType().getMemberScope(), "next");
        PropertyDescriptor hasNextProperty = getPropertyByName(iteratorClass.getDefaultType().getMemberScope(), "hasNext");
        aliaser.setAliasForDescriptor(nextFunction, namer.libraryMethod("next"));
        aliaser.setAliasForDescriptor(hasNextProperty, namer.libraryMethod("hasNext"));
    }

    private static void setAliasesForArray(JetStandardLibrary standardLibrary, Namer namer, Aliaser aliaser) {
        aliaser.setAliasForDescriptor(standardLibrary.getArray(), namer.libraryObject("Array"));
        FunctionDescriptor nullConstructorFunction = getFunctionByName(standardLibrary.getLibraryScope(), "Array");
        aliaser.setAliasForDescriptor(nullConstructorFunction, namer.libraryObject("array"));
        PropertyDescriptor sizeProperty =
                getPropertyByName(standardLibrary.getArray().getDefaultType().getMemberScope(), "size");
        aliaser.setAliasForDescriptor(sizeProperty, namer.libraryMethod("size"));
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