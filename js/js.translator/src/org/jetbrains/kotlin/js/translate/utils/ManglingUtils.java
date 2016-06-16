/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.js.naming.FQNGenerator;
import org.jetbrains.kotlin.name.Name;

import java.util.Collection;

public class ManglingUtils {
    private ManglingUtils() {}

    @NotNull
    public static String getStableMangledNameForDescriptor(@NotNull ClassDescriptor descriptor, @NotNull String functionName) {
        Collection<SimpleFunctionDescriptor> functions = descriptor.getDefaultType().getMemberScope().getContributedFunctions(
                Name.identifier(functionName), NoLookupLocation.FROM_BACKEND);
        assert functions.size() == 1 : "Can't select a single function: " + functionName + " in " + descriptor;
        return new FQNGenerator().generate(functions.iterator().next()).getNames().get(0);
    }
}
