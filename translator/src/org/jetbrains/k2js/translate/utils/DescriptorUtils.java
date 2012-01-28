package org.jetbrains.k2js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
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

    public static boolean isConstructorDescriptor(@NotNull CallableDescriptor descriptor) {
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
                "In scope " + scope + " supposed to be exactly one " + name + " function.\n" +
                        "Found: " + functionDescriptors.size();
        //noinspection LoopStatementThatDoesntLoop
        for (FunctionDescriptor descriptor : functionDescriptors) {
            return descriptor;
        }
        throw new AssertionError("In scope " + scope
                + " supposed to be exactly one " + name + " function.");
    }

    //TODO: some stange stuff happening to this method
    @NotNull
    public static PropertyDescriptor getPropertyByName(@NotNull JetScope scope,
                                                       @NotNull String name) {
        VariableDescriptor variable = scope.getLocalVariable(name);
        if (variable == null) {
            variable = scope.getPropertyByFieldReference("$" + name);
        }
        Set<VariableDescriptor> variables = scope.getProperties(name);
        assert variables.size() == 1 : "Actual size: " + variables.size();
        variable = variables.iterator().next();
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

    public static boolean isExtensionFunction(@NotNull CallableDescriptor functionDescriptor) {
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

    //TODO: why callable descriptor
    @Nullable
    public static DeclarationDescriptor getExpectedThisDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverDescriptor expectedThisObject = callableDescriptor.getExpectedThisObject();
        if (!expectedThisObject.exists()) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(expectedThisObject);
    }

    @NotNull
    public static DeclarationDescriptor getDeclarationDescriptorForReceiver
            (@NotNull ReceiverDescriptor receiverParameter) {
        DeclarationDescriptor declarationDescriptor =
                receiverParameter.getType().getConstructor().getDeclarationDescriptor();
        //TODO: WHY assert?
        assert declarationDescriptor != null;
        return declarationDescriptor.getOriginal();
    }

    @Nullable
    public static DeclarationDescriptor getExpectedReceiverDescriptor(@NotNull CallableDescriptor callableDescriptor) {
        ReceiverDescriptor receiverParameter = callableDescriptor.getReceiverParameter();
        if (!receiverParameter.exists()) {
            return null;
        }
        return getDeclarationDescriptorForReceiver(receiverParameter);
    }

    //TODO: maybe we have similar routine
    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        while (containing != null) {
            if (containing instanceof ClassDescriptor) {
                return (ClassDescriptor) containing;
            }
            containing = containing.getContainingDeclaration();
        }
        return null;
    }

}
