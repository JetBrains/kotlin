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

package org.jetbrains.k2js.translate.intrinsic.primitive;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.EqualsIntrinsic;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.equality;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.inequality;

/**
 * @author Pavel Talanov
 */
public final class PrimitiveEqualsIntrinsic extends EqualsIntrinsic {

    @NotNull
    public static PrimitiveEqualsIntrinsic newInstance() {
        return new PrimitiveEqualsIntrinsic();
    }

    private PrimitiveEqualsIntrinsic() {
    }

    @NotNull
    public JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
                              @NotNull TranslationContext context) {
        assert arguments.size() == 1 : "Equals operation should have one argument";
        assert receiver != null;
        if (isNegated()) {
            return inequality(receiver, arguments.get(0));
        }
        else {
            return equality(receiver, arguments.get(0));
        }
    }

}
