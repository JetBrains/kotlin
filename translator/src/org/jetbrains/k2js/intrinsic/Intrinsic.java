package org.jetbrains.k2js.intrinsic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

/**
 * @author Talanov Pavel
 */
public interface Intrinsic {

    @NotNull
    JsExpression apply(@NotNull FunctionDescriptor descriptor, @NotNull JetExpression receiver,
                       @NotNull List<JetExpression> arguments);

}
