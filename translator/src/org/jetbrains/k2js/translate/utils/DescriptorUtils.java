package org.jetbrains.k2js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Pavel Talanov
 */
public final class DescriptorUtils {

    private DescriptorUtils() {
    }

    private static int valueParametersCount(@NotNull FunctionDescriptor functionDescriptor) {
        return functionDescriptor.getValueParameters().size();
    }

    public static boolean hasParameters(@NotNull FunctionDescriptor functionDescriptor) {
        return (valueParametersCount(functionDescriptor) > 0);
    }

    public static boolean isEquals(@NotNull FunctionDescriptor functionDescriptor) {
        return (functionDescriptor.getName().equals(OperatorConventions.EQUALS));
    }

    public static boolean isCompareTo(@NotNull FunctionDescriptor functionDescriptor) {
        return (functionDescriptor.getName().equals(OperatorConventions.COMPARE_TO));
    }

    public static boolean isConstructorDescriptor(@NotNull FunctionDescriptor descriptor) {
        return (descriptor instanceof ConstructorDescriptor);
    }

    @NotNull
    public static List<DeclarationDescriptor> getOwnDeclarations(@NotNull ClassDescriptor classDescriptor) {
        Collection<DeclarationDescriptor> allDescriptors =
                classDescriptor.getDefaultType().getMemberScope().getAllDescriptors();

        return filterByOwner(classDescriptor, allDescriptors);
    }

    @NotNull
    private static List<DeclarationDescriptor> filterByOwner(@NotNull DeclarationDescriptor ownerDescriptor,
                                                             @NotNull Collection<DeclarationDescriptor> allDescriptors) {
        List<DeclarationDescriptor> resultingList = new ArrayList<DeclarationDescriptor>();
        for (DeclarationDescriptor memberDescriptor : allDescriptors) {
            if (memberDescriptor.getContainingDeclaration() == ownerDescriptor) {
                resultingList.add(memberDescriptor);
            }
        }
        return resultingList;
    }

    @NotNull
    public static FunctionDescriptor getFunctionByName(@NotNull ClassDescriptor classDescriptor,
                                                       @NotNull String name) {
        JetScope scope = classDescriptor.getDefaultType().getMemberScope();
        return getFunctionByName(scope, name);
    }

    @NotNull
    public static FunctionDescriptor getFunctionByName(@NotNull JetScope scope,
                                                       @NotNull String name) {
        Set<FunctionDescriptor> functionDescriptors = scope.getFunctions(name);
        assert functionDescriptors.size() == 1 :
                "In scope " + scope + " supposed to be exactly one " + name + " function.";
        //noinspection LoopStatementThatDoesntLoop
        for (FunctionDescriptor descriptor : functionDescriptors) {
            return descriptor;
        }
        throw new AssertionError("In scope " + scope
                + " supposed to be exactly one " + name + " function.");
    }

    @NotNull
    public static PropertyDescriptor getPropertyByName(@NotNull JetScope scope,
                                                       @NotNull String name) {
        VariableDescriptor variable = scope.getLocalVariable(name);
        if (variable == null) {
            variable = scope.getPropertyByFieldReference("$" + name);
        }
        PropertyDescriptor descriptor = (PropertyDescriptor) variable;
        assert descriptor != null : "Must have a descriptor.";
        return descriptor;
    }

    @Nullable
    public static ClassDescriptor findAncestorClass(@NotNull List<ClassDescriptor> superclassDescriptors) {
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() == ClassKind.CLASS) {
                return descriptor;
            }
        }
        return null;
    }

    @NotNull
    public static VariableDescriptor getVariableDescriptorForVariableAsFunction
            (@NotNull VariableAsFunctionDescriptor descriptor) {
        VariableDescriptor functionVariable = descriptor.getVariableDescriptor();
        assert functionVariable != null;
        return functionVariable;
    }


    public static boolean isVariableDescriptor(@Nullable DeclarationDescriptor referencedDescriptor) {
        return referencedDescriptor instanceof VariableAsFunctionDescriptor;
    }

    @NotNull
    public static DeclarationDescriptor getContainingDeclaration(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        assert containing != null : "Should be called on objects that have containing declaration.";
        return containing;
    }

    public static boolean isExtensionFunction(@NotNull FunctionDescriptor functionDescriptor) {
        return (functionDescriptor.getReceiverParameter().exists());
    }

    //TODO: make "anonymous" a constant
    @NotNull
    public static String getNameForNamespace(@NotNull NamespaceDescriptor descriptor) {
        String name = descriptor.getName();
        if (name.equals("")) {
            return "Anonymous";
        }
        return name;
    }
}
