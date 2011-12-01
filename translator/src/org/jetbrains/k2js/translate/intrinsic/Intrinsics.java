package org.jetbrains.k2js.translate.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.k2js.translate.operation.OperatorTable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class Intrinsics {

    @NotNull
    private final Map<FunctionDescriptor, Intrinsic> descriptorToIntrinsicMap =
            new HashMap<FunctionDescriptor, Intrinsic>();

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
        //TODO: HACK!
        addCompareToIntrinsics(descriptor);
        addEqualsIntrinsics(descriptor);
        addUnaryIntrinsics(descriptor);
        addBinaryIntrinsics(descriptor);

    }

    private void addCompareToIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (functionName.equals("compareTo")) {
            descriptorToIntrinsicMap.put(descriptor, PrimitiveCompareToIntrinsic.newInstance());
        }
    }

    private void addEqualsIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (functionName.equals("equals")) {
            descriptorToIntrinsicMap.put(descriptor, PrimitiveEqualsIntrinsic.newInstance());
        }
    }

    private void addUnaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        JetToken token = OperatorConventions.UNARY_OPERATION_NAMES.inverse().get(functionName);
        if (token != null) {
            descriptorToIntrinsicMap.put(descriptor, UnaryOperationIntrinsic.newInstance(token));
        }
    }

    private void addBinaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        JetToken token = OperatorConventions.BINARY_OPERATION_NAMES.inverse().get(functionName);
        if (token != null && OperatorTable.hasCorrespondingBinaryOperator(token)) {
            descriptorToIntrinsicMap.put(descriptor, BinaryOperationIntrinsic.newInstance(token));
        }
    }

    public boolean hasDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor.getOriginal();
            return (descriptorToIntrinsicMap.get(functionDescriptor) != null);
        }
        return false;
    }


    @NotNull
    public Intrinsic getIntrinsic(@NotNull DeclarationDescriptor descriptor) {
        assert descriptor instanceof FunctionDescriptor;
        Intrinsic intrinsic = descriptorToIntrinsicMap.get(descriptor);
        return intrinsic;
    }
}
