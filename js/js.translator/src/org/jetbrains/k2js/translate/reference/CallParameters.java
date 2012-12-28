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

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CallParameters {

    @Nullable
    private final JsExpression receiver;

    @NotNull
    private final JsExpression functionReference;

    @Nullable
    private final JsExpression thisObject;

    public CallParameters(@Nullable JsExpression receiver,
                          @NotNull JsExpression functionReference,
                          @Nullable JsExpression thisObject) {
        this.receiver = receiver;
        this.functionReference = functionReference;
        this.thisObject = thisObject;
    }

    @NotNull
    public JsExpression getFunctionReference() {
        return functionReference;
    }

    @Nullable
    public JsExpression getThisObject() {
        return thisObject;
    }

    @Nullable
    public JsExpression getReceiver() {
        return receiver;
    }

    @Nullable
    public JsExpression getThisOrReceiverOrNull() {
        if (thisObject == null) {
            return receiver;
        }
        assert receiver == null;
        return thisObject;
    }
}
