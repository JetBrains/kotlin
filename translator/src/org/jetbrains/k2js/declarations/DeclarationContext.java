package org.jetbrains.k2js.declarations;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeclarationContext {

    @NotNull
    public static DeclarationContext rootContext(@NotNull JsScope scope, @Nullable JsNameRef qualifier) {
        return new DeclarationContext(scope, qualifier);
    }

    @NotNull
    private final JsScope scope;

    @Nullable
    private final JsNameRef qualifier;

    private DeclarationContext(@NotNull JsScope scope, @Nullable JsNameRef qualifier) {
        this.scope = scope;
        this.qualifier = qualifier;
    }

    @NotNull
    public JsScope getScope() {
        return scope;
    }

    @Nullable
    public JsNameRef getQualifier() {
        return qualifier;
    }

    @NotNull
    public DeclarationContext innerDeclaration(@NotNull JsScope declarationScope, @NotNull JsName declarationName) {
        JsNameRef reference = declarationName.makeRef();
        if (qualifier != null) {
            AstUtil.setQualifier(reference, qualifier);
        }
        return new DeclarationContext(declarationScope, reference);
    }
}