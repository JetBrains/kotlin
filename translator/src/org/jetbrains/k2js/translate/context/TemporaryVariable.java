package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public final class TemporaryVariable {

    @NotNull
    private final JsExpression assignmentExpression;
    @NotNull
    private final JsName variableName;

    /*package*/ TemporaryVariable(@NotNull JsName temporaryName, @NotNull JsExpression initExpression) {
        this.variableName = temporaryName;
        this.assignmentExpression = AstUtil.newAssignment(variableName.makeRef(), initExpression);
    }

    @NotNull
    public JsNameRef nameReference() {
        return variableName.makeRef();
    }

    @NotNull
    public JsName name() {
        return variableName;
    }

    @NotNull
    public JsExpression assignmentExpression() {
        return assignmentExpression;
    }
}