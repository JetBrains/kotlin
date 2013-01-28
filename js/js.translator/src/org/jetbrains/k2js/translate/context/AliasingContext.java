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

import com.google.common.collect.Maps;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsLiteral;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver;

import java.util.Map;

import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getDeclarationDescriptorForReceiver;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getExpectedReceiverDescriptor;

public class AliasingContext {
    private static final ThisAliasProvider EMPTY_THIS_ALIAS_PROVIDER = new ThisAliasProvider() {
        @Nullable
        @Override
        public JsNameRef get(@NotNull DeclarationDescriptor descriptor) {
            return null;
        }

        @Nullable
        @Override
        public JsExpression get(@NotNull ResolvedCall<?> call) {
            ReceiverValue callThisObject = call.getThisObject();
            return callThisObject.exists() && (callThisObject instanceof ClassReceiver || callThisObject instanceof ExtensionReceiver)
                   ? JsLiteral.THIS
                   : null;
        }
    };

    private static final AliasingContext ROOT = new AliasingContext(null, EMPTY_THIS_ALIAS_PROVIDER) {
        @Override
        public JsName getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
            return null;
        }

        @Override
        public JsName getAliasForExpression(@NotNull JetExpression element) {
            return null;
        }
    };

    public static AliasingContext getCleanContext() {
        return new AliasingContext(ROOT, ROOT.thisAliasProvider);
    }

    @NotNull
    private final Map<DeclarationDescriptor, JsName> aliasesForDescriptors = Maps.newHashMap();

    @NotNull
    final ThisAliasProvider thisAliasProvider;
    @NotNull
    private final Map<JetExpression, JsName> aliasesForExpressions = Maps.newHashMap();

    @Nullable
    private final AliasingContext parent;

    private AliasingContext(@Nullable AliasingContext parent, @NotNull ThisAliasProvider thisAliasProvider) {
        this.parent = parent;
        this.thisAliasProvider = thisAliasProvider;
    }

    public interface ThisAliasProvider {
        @Nullable
        JsNameRef get(@NotNull DeclarationDescriptor descriptor);
        @Nullable
        JsExpression get(@NotNull ResolvedCall<?> call);
    }

    public abstract static class AbstractThisAliasProvider implements ThisAliasProvider {
        @NotNull
        protected static DeclarationDescriptor normalize(@NotNull DeclarationDescriptor descriptor) {
            if (descriptor instanceof ClassOrNamespaceDescriptor) {
                return descriptor;
            }
            else if (descriptor instanceof CallableDescriptor) {
                DeclarationDescriptor receiverDescriptor = getExpectedReceiverDescriptor((CallableDescriptor) descriptor);
                assert receiverDescriptor != null;
                return receiverDescriptor;
            }

            return descriptor;
        }

        @Nullable
        @Override
        public JsExpression get(@NotNull ResolvedCall<?> call) {
            ReceiverValue thisObject = call.getThisObject();
            if (!thisObject.exists()) {
                return null;
            }

            if (thisObject instanceof ExtensionReceiver || thisObject instanceof ClassReceiver) {
                JsNameRef ref = get(((ThisReceiver) thisObject).getDeclarationDescriptor());
                if (ref != null) {
                    return ref;
                }
            }

            JsNameRef ref = get(getDeclarationDescriptorForReceiver(thisObject));
            return ref == null ? JsLiteral.THIS : ref;
        }
    }

    @NotNull
    public AliasingContext inner(@NotNull ThisAliasProvider thisAliasProvider) {
        return new AliasingContext(this, thisAliasProvider);
    }

    @NotNull
    public AliasingContext inner(@NotNull final DeclarationDescriptor correspondingDescriptor, @NotNull final JsName alias) {
        return inner(new AbstractThisAliasProvider() {
            @Nullable
            @Override
            public JsNameRef get(@NotNull DeclarationDescriptor descriptor) {
                return correspondingDescriptor == normalize(descriptor) ? alias.makeRef() : null;
            }
        });
    }

    @NotNull
    public AliasingContext withAliasesForExpressions(@NotNull Map<JetExpression, JsName> aliasesForExpressions) {
        AliasingContext newContext = new AliasingContext(this, thisAliasProvider);
        newContext.aliasesForExpressions.putAll(aliasesForExpressions);
        return newContext;
    }

    @NotNull
    public AliasingContext withDescriptorsAliased(@NotNull Map<DeclarationDescriptor, JsName> aliases) {
        AliasingContext newContext = new AliasingContext(this, thisAliasProvider);
        newContext.aliasesForDescriptors.putAll(aliases);
        return newContext;
    }

    @Nullable
    public JsName getAliasForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        JsName alias = aliasesForDescriptors.get(descriptor.getOriginal());
        if (alias != null) {
            return alias;
        }
        assert parent != null;
        return parent.getAliasForDescriptor(descriptor);
    }

    @Nullable
    public JsName getAliasForExpression(@NotNull JetExpression element) {
        JsName alias = aliasesForExpressions.get(element);
        if (alias != null) {
            return alias;
        }
        assert parent != null;
        return parent.getAliasForExpression(element);
    }
}
