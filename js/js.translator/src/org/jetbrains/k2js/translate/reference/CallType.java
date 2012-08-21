/*
/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsConditional;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsLiteral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetSafeQualifiedExpression;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.TranslationUtils.notNullConditionalTestExpression;

/**
 * @author Pavel Talanov
 */
public enum CallType {
    SAFE {
        @NotNull
        @Override
        JsExpression constructCall(@Nullable JsExpression receiver, @NotNull CallConstructor constructor,
                                   @NotNull TranslationContext context) {
            assert receiver != null;
            TemporaryVariable cachedValue = context.declareTemporary(receiver);
            return new JsConditional(notNullConditionalTestExpression(cachedValue), constructor.construct(cachedValue.reference()), JsLiteral.NULL);
        }
    },
    //TODO: bang qualifier is not implemented in frontend for now
    // BANG,
    NORMAL {
        @NotNull
        @Override
        JsExpression constructCall(@Nullable JsExpression receiver, @NotNull CallConstructor constructor,
                                   @NotNull TranslationContext context) {
            return constructor.construct(receiver);
        }
    };

    @NotNull
    abstract JsExpression constructCall(@Nullable JsExpression receiver, @NotNull CallConstructor constructor,
                                        @NotNull TranslationContext context);

    @NotNull
    public static CallType getCallTypeForQualifiedExpression(@NotNull JetQualifiedExpression expression) {
        if (expression instanceof JetSafeQualifiedExpression) {
            return SAFE;
        }
        assert expression instanceof JetDotQualifiedExpression;
        return NORMAL;
    }

    public interface CallConstructor {
        @NotNull
        JsExpression construct(@Nullable JsExpression receiver);
    }

}
