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
    private final Map<DeclarationDescriptor, Intrinsic> descriptorToIntrinsicMap =
            new HashMap<DeclarationDescriptor, Intrinsic>();

    public static Intrinsics standardLibraryIntrinsics(@NotNull JetStandardLibrary library) {
        return new Intrinsics(library);
    }

    private Intrinsics(@NotNull JetStandardLibrary library) {
        IntrinsicDeclarationVisitor visitor = new IntrinsicDeclarationVisitor(this);
        for (DeclarationDescriptor descriptor : library.getLibraryScope().getAllDescriptors()) {
            descriptor.accept(visitor, null);
        }
    }

    /*package*/ void declareIntrinsic(@NotNull DeclarationDescriptor descriptor) {
        //TODO: this is a hack
        descriptorToIntrinsicMap.put(descriptor, null);
        addBinaryIntrinsics(descriptor);
//        addUnaryIntrinsics(descriptor);
//        addAssignmentIntrinsics(descriptor);
//        addEqualsIntrinsics(descriptor);
    }

    //    private void addEqualsIntrinsics(FunctionDescriptor descriptor) {
//        //BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
//        String functionName = descriptor.getName();
//        if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsValue(functionName)) {
//            descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
//        }
//    }
//
//    private void addUnaryIntrinsics(@NotNull FunctionDescriptor descriptor) {
//        BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
//        String functionName = descriptor.getName();
//        if (OperatorConventions.UNARY_OPERATION_NAMES.containsValue(functionName)) {
//            descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
//        }
//    }
//
    private void addBinaryIntrinsics(@NotNull DeclarationDescriptor descriptor) {
        String functionName = descriptor.getName();
        if (OperatorConventions.BINARY_OPERATION_NAMES.containsValue(functionName)) {
            descriptorToIntrinsicMap.put(descriptor, BinaryOperationIntrinsic.INSTANCE);
        }
    }

    //
//    private void addAssignmentIntrinsics(@NotNull FunctionDescriptor descriptor) {
//        String functionName = descriptor.getName();
//        if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsValue(functionName)) {
//            BinaryOperationIntrinsic binaryOperationIntrinsic = new BinaryOperationIntrinsic();
//            descriptorToIntrinsicMap.put(descriptor, binaryOperationIntrinsic);
//        }
//    }
//
    public boolean hasDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor.getOriginal();
            return descriptorToIntrinsicMap.containsKey(functionDescriptor);
        }
        return false;
    }

    public Intrinsic getIntrinsic(@NotNull DeclarationDescriptor descriptor) {
        Intrinsic intrinsic = descriptorToIntrinsicMap.get(descriptor);
//        assert intrinsic != null;
        return intrinsic;
    }
}
