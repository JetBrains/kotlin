package org.jetbrains.k2js.intrinsic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;

/**
 * @author Talanov Pavel
 */
public final class IntrinsicDeclarationVisitor extends DeclarationDescriptorVisitor<Void, Void> {

    @NotNull
    private final Intrinsics intrinsics;

    /*package*/ IntrinsicDeclarationVisitor(@NotNull Intrinsics intrinsics) {
        this.intrinsics = intrinsics;
    }

    @Override
    public Void visitClassDescriptor(@NotNull ClassDescriptor descriptor, @Nullable Void nothing) {
        for (DeclarationDescriptor memberDescriptor :
                descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            memberDescriptor.accept(this, null);
        }
        return null;
    }

    @Override
    public Void visitFunctionDescriptor(@NotNull FunctionDescriptor descriptor, @Nullable Void nothing) {
        intrinsics.declareIntrinsic(descriptor);
        return null;
    }
}
