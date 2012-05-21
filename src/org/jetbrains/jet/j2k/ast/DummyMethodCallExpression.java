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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public class DummyMethodCallExpression extends Expression {
    private final Element myWho;
    private final String myMethodName;
    private final Element myWhat;

    public DummyMethodCallExpression(Element who, String methodName, Element what) {
        myWho = who;
        myMethodName = methodName;
        myWhat = what;
    }

    @NotNull
    @Override
    public String toKotlin() {
        return myWho.toKotlin() + DOT + myMethodName + "(" + myWhat.toKotlin() + ")";
    }
}
