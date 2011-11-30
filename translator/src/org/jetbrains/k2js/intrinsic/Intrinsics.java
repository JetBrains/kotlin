package org.jetbrains.k2js.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;

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
        //TODO: this is a hack
//        addBinaryIntrinsics(descriptor);
//        addUnaryIntrinsics(descriptor);
//        addAssignmentIntrinsics(descriptor);
//        addEqualsIntrinsics(descriptor);
        BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
        descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
    }

    private void addEqualsIntrinsics(FunctionDescriptor descriptor) {
        BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
        String functionName = descriptor.getName();
        if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsValue(functionName)) {
            descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
        }
    }

    private void addUnaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
        String functionName = descriptor.getName();
        if (OperatorConventions.UNARY_OPERATION_NAMES.containsValue(functionName)) {
            descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
        }
    }

    private void addBinaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (OperatorConventions.BINARY_OPERATION_NAMES.containsValue(functionName)) {
            BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
            descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
        }
    }

    private void addAssignmentIntrinsics(@NotNull FunctionDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsValue(functionName)) {
            BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
            descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
        }
    }

    public boolean isIntrinsic(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
            return descriptorToIntrinsicMap.containsKey(functionDescriptor);
        }
        return false;
    }
}
