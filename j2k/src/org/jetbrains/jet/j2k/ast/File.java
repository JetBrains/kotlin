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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

public class File extends Node {
    private final String myPackageName;
    private final List<Import> myImports;
    private final List<Class> myClasses;
    private final String myMainFunction;

    public File(String packageName, List<Import> imports, List<Class> classes, String mainFunction) {
        myPackageName = packageName;
        myImports = imports;
        myClasses = classes;
        myMainFunction = mainFunction;
    }

    @NotNull
    @Override
    public String toKotlin() {
        String common = AstUtil.joinNodes(myImports, N) + N2 + AstUtil.joinNodes(myClasses, N) + N + myMainFunction;
        if (myPackageName.isEmpty()) {
            return common;
        }
        return "package" + SPACE + myPackageName + N +
               common;
    }
}
