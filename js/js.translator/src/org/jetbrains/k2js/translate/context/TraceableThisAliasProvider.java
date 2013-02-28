/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassOrPackageDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

public class TraceableThisAliasProvider extends AliasingContext.AbstractThisAliasProvider {
    private final ClassOrPackageDescriptor descriptor;
    private final JsNameRef thisRef;
    private boolean thisWasCaptured;

    public boolean wasThisCaptured() {
        return thisWasCaptured;
    }

    public TraceableThisAliasProvider(@NotNull ClassOrPackageDescriptor descriptor, @NotNull JsNameRef thisRef) {
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
