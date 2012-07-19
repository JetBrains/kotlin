package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

public class TraceableThisAliasProvider extends AliasingContext.AbstractThisAliasProvider {
    private final ClassOrNamespaceDescriptor descriptor;
    private final JsNameRef thisRef;
    private boolean thisWasCaptured;

    public boolean wasThisCaptured() {
        return thisWasCaptured;
    }

    public TraceableThisAliasProvider(@NotNull ClassOrNamespaceDescriptor descriptor, @NotNull JsNameRef thisRef) {
        this.descriptor = descriptor;
        this.thisRef = thisRef;
    }

    @Nullable
    public JsNameRef getRefIfWasCaptured() {
        return thisWasCaptured ? thisRef : null;
    }

    @Nullable
    @Override
    public JsNameRef get(@NotNull DeclarationDescriptor unnormalizedDescriptor) {
        if (descriptor == normalize(unnormalizedDescriptor)) {
            thisWasCaptured = true;
            return thisRef;
        }

        return null;
    }
}
