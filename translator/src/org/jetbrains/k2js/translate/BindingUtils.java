package org.jetbrains.k2js.translate;

import com.intellij.codeInsight.hint.ClassDeclarationRangeHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;

import javax.xml.ws.Binding;

/**
 * @author Talanov Pavel
 *
 * This class contains some code related to BindingContext use. Intention is not to pollute other classes.
 */
public final class BindingUtils {
    private BindingUtils() {}

    //TODO generalise methods?
    @NotNull
    static public ClassDescriptor getClassDescriptor(@NotNull BindingContext context, @NotNull JetClass declaration) {
                DeclarationDescriptor descriptor
                = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        assert descriptor instanceof ClassDescriptor : "Class should have a descriptor" +
                " of type ClassDescriptor";
        return (ClassDescriptor)descriptor;
    }

    @NotNull
    static public NamespaceDescriptor getNamespaceDescriptor(@NotNull BindingContext context,
                                                             @NotNull JetNamespace declaration) {
        DeclarationDescriptor descriptor
                = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        assert descriptor instanceof NamespaceDescriptor : "Namespace should have a descriptor" +
                " of type NamespaceDescriptor";
        return (NamespaceDescriptor)descriptor;
    }

    @NotNull
    static public FunctionDescriptor getFunctionDescriptor(@NotNull BindingContext context,
                                                           @NotNull JetNamedFunction declaration) {
        DeclarationDescriptor descriptor =
                context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        assert descriptor instanceof FunctionDescriptor : "JetNamedFunction should have" +
                " descriptor of type FunctionDescriptor.";
        return (FunctionDescriptor)descriptor;
    }

}
