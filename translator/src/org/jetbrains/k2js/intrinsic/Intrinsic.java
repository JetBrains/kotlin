package org.jetbrains.k2js.intrinsic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.k2js.translate.general.TranslationContext;

/**
 * @author Talanov Pavel
 */
public interface Intrinsic {

    @NotNull
    public JsExpression apply(@NotNull JetExpression expression,
                              @NotNull TranslationContext context);

}
