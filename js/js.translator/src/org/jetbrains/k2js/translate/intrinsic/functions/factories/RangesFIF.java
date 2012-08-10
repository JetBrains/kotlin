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

package org.jetbrains.k2js.translate.intrinsic.functions.factories;

import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.intrinsic.functions.basic.CallStandardMethodIntrinsic;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;

/**
 * @author Pavel Talanov
 */
public final class RangesFIF extends CompositeFIF {

    @NotNull
    public static final FunctionIntrinsicFactory INSTANCE = new RangesFIF();

    private RangesFIF() {
        add(pattern("Int.upto"), new CallStandardMethodIntrinsic(new JsNameRef("intUpto", "Kotlin"), true, 1));
        add(pattern("Int.downto"), new CallStandardMethodIntrinsic(new JsNameRef("intDownto", "Kotlin"), true, 1));
    }
}
