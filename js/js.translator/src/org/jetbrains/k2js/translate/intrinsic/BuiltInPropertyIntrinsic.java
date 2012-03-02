package org.jetbrains.k2js.translate.intrinsic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.setQualifier;

/**
 * @author Pavel Talanov
 */
//TODO: find should be usages
public final class BuiltInPropertyIntrinsic implements Intrinsic {

    @NotNull
    private final String propertyName;

    public BuiltInPropertyIntrinsic(@NotNull String propertyName) {
        this.propertyName = propertyName;
    }

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert receiver != null;
        assert arguments.isEmpty() : "Properties can't have arguments.";
        JsNameRef propertyReference = AstUtil.newQualifiedNameRef(propertyName);
        setQualifier(propertyReference, receiver);
        return propertyReference;
    }
}