package org.jetbrains.k2js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Talanov Pavel
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
}
