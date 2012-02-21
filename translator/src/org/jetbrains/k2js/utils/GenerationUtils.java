/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

//TODO: very thin class

/**
 * @author Pavel Talanov
 */
public final class GenerationUtils {

    @NotNull
    public static String generateCallToMain(@NotNull String namespaceName, @NotNull List<String> arguments) {
        String constructArguments = "var args = [];\n";
        int index = 0;
        for (String argument : arguments) {
            constructArguments = constructArguments + "args[" + index + "]= \"" + argument + "\";\n";
            index++;
        }
        String callMain = namespaceName + ".main(args);\n";
        return constructArguments + callMain;
    }


}
