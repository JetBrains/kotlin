package org.jetbrains.k2js.translate.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.operation.OperatorTable;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
//TODO: different maps for different kinds of intrinsics
public final class Intrinsics {

    @NotNull
    private final Map<FunctionDescriptor, FunctionIntrinsic> functionIntrinsics =
            new HashMap<FunctionDescriptor, FunctionIntrinsic>();

    @NotNull
    private final Map<FunctionDescriptor, EqualsIntrinsic> equalsIntrinsics =
            new HashMap<FunctionDescriptor, EqualsIntrinsic>();


    @NotNull
    private final Map<FunctionDescriptor, CompareToIntrinsic> compareToIntrinsics =
            new HashMap<FunctionDescriptor, CompareToIntrinsic>();

    public static Intrinsics standardLibraryIntrinsics(@NotNull JetStandardLibrary library) {
        return new Intrinsics(library);
    }

    private Intrinsics(@NotNull JetStandardLibrary library) {
        IntrinsicDeclarationVisitor visitor = new IntrinsicDeclarationVisitor(this);
        for (DeclarationDescriptor descriptor : library.getLibraryScope().getAllDescriptors()) {
            descriptor.accept(visitor, null);
        }
    }

    /*package*/ void declareIntrinsic(@NotNull FunctionDescriptor descriptor) {
        addCompareToIntrinsics(descriptor);
        addEqualsIntrinsics(descriptor);
        addUnaryIntrinsics(descriptor);
        addBinaryIntrinsics(descriptor);
    }

    private void addCompareToIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (functionName.equals("compareTo")) {
            compareToIntrinsics.put(descriptor, PrimitiveCompareToIntrinsic.newInstance());
        }
    }

    private void addEqualsIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (functionName.equals("equals")) {
            equalsIntrinsics.put(descriptor, PrimitiveEqualsIntrinsic.newInstance());
        }
    }

    private void addUnaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        JetToken token = OperatorConventions.UNARY_OPERATION_NAMES.inverse().get(functionName);
        if (token == null) return;
        boolean isUnary = !DescriptorUtils.hasParameters(descriptor);
        if (!isUnary) return;
        functionIntrinsics.put(descriptor, UnaryOperationIntrinsic.newInstance(token));
    }

    private void addBinaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        boolean isUnary = !DescriptorUtils.hasParameters(descriptor);
        if (isUnary) return;
        JetToken token = OperatorConventions.BINARY_OPERATION_NAMES.inverse().get(functionName);
        if (token == null) return;
        //TODO: implement range and contains intrinsic
        if (!OperatorTable.hasCorrespondingBinaryOperator(token)) return;
        functionIntrinsics.put(descriptor, BinaryOperationIntrinsic.newInstance(token));
    }

    public boolean isIntrinsic(@NotNull DeclarationDescriptor descriptor) {
        //NOTE: that if we want to add other intrinsics we have to modify this method
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor.getOriginal();
            return (equalsIntrinsics.containsKey(functionDescriptor) ||
                    compareToIntrinsics.containsKey(functionDescriptor) ||
                    functionIntrinsics.containsKey(functionDescriptor));
        }
        return false;
    }

    @NotNull
    public FunctionIntrinsic getFunctionIntrinsic(@NotNull FunctionDescriptor descriptor) {
        return functionIntrinsics.get(descriptor);
    }

    @NotNull
    public CompareToIntrinsic getCompareToIntrinsic(@NotNull FunctionDescriptor descriptor) {
        return compareToIntrinsics.get(descriptor);
    }

    @NotNull
    public EqualsIntrinsic getEqualsIntrinsic(@NotNull FunctionDescriptor descriptor) {
        return equalsIntrinsics.get(descriptor);
    }
}
