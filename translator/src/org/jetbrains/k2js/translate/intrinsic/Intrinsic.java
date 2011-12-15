package org.jetbrains.k2js.translate.intrinsic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public interface Intrinsic {

    @NotNull
    JsExpression apply(@NotNull JsExpression receiver, @NotNull List<JsExpression> arguments,
                       @NotNull TranslationContext context);
}
