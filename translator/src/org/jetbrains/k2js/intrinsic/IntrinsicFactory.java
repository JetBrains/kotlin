package org.jetbrains.k2js.intrinsic;

import org.jetbrains.annotations.NotNull;

/**
 * @author Talanov Pavel
 */
public final class IntrinsicFactory {

    @NotNull
    private final Intrinsics intrinsics;

    private IntrinsicFactory(@NotNull Intrinsics intrinsics) {
        this.intrinsics = intrinsics;
    }

//    public Intrinsic getIntrinsicForExpression(@NotNull FunctionDescriptor descriptor,
//                                               @NotNull JetExpression expression,
//                                               @NotNull TranslationContext context) {
//        assert intrinsics.hasDescriptor(descriptor);
//        if (JetExpression instanceof JetBinaryExpression) {
//            return new BinaryOperationIntrinsic()
//        }
//    }


}
