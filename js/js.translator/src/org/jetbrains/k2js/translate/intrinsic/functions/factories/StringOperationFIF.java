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

package org.jetbrains.k2js.translate.intrinsic.functions.factories;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.BuiltInFunctionIntrinsic;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

public final class StringOperationFIF extends CompositeFIF {
    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new StringOperationFIF();

    private StringOperationFIF() {
        add(pattern("kotlin", "CharSequence", "get").checkOverridden(), new BuiltInFunctionIntrinsic("charAt"));
        add(pattern("kotlin.js", "length").isExtensionOf("kotlin.CharSequence"), LENGTH_PROPERTY_INTRINSIC);
        add(pattern("kotlin.js", "isEmpty").isExtensionOf("kotlin.CharSequence"), IS_EMPTY_INTRINSIC);
    }
}
