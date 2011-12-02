package org.jetbrains.k2js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;

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

}
