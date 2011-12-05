package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeclarationContext {

    @NotNull
    public static DeclarationContext rootContext(@NotNull NamingScope scope, @Nullable JsNameRef qualifier) {
        return new DeclarationContext(scope, qualifier);
    }

    @NotNull
    private final NamingScope scope;

    @Nullable
    private final JsNameRef qualifier;

    private DeclarationContext(@NotNull NamingScope scope, @Nullable JsNameRef qualifier) {
        this.scope = scope;
        this.qualifier = qualifier;
    }

    @NotNull
    public NamingScope getScope() {
        return scope;
    }

    @Nullable
    public JsNameRef getQualifier() {
        return qualifier;
    }

    @NotNull
    public DeclarationContext innerDeclaration(@NotNull NamingScope declarationScope, @NotNull JsName declarationName) {
        JsNameRef reference = declarationName.makeRef();
        if (qualifier != null) {
            AstUtil.setQualifier(reference, qualifier);
        }
        return new DeclarationContext(declarationScope, reference);
    }
}