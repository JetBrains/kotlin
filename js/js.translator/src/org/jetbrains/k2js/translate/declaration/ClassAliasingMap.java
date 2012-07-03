package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;

public interface ClassAliasingMap {
    @Nullable
    JsNameRef get(JetClass declaration, JetClass referencedDeclaration);
}
