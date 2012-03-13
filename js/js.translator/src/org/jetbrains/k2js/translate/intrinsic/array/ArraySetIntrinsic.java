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

package org.jetbrains.k2js.translate.intrinsic.array;

import com.google.dart.compiler.backend.js.ast.JsArrayAccess;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.Intrinsic;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public enum ArraySetIntrinsic implements Intrinsic {

    INSTANCE;

    @NotNull
    @Override
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert receiver != null;
        assert arguments.size() == 2 : "Array set expression must have two arguments.";
        JsExpression indexExpression = arguments.get(0);
        JsExpression value = arguments.get(1);
        JsArrayAccess arrayAccess = AstUtil.newArrayAccess(receiver, indexExpression);
        return AstUtil.newAssignment(arrayAccess, value);
    }
}
