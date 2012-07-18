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

package org.jetbrains.k2js.translate.intrinsic.functions.patterns;

import closurecompiler.internal.com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class NameChecker {

    @NotNull
    public static final NameChecker PRIMITIVE_NUMBERS = new NameChecker("Int", "Double", "Float", "Long", "Short");

    @NotNull
    private final List<Name> validNames = Lists.newArrayList();

    public NameChecker(@NotNull String... validNames) {
        this(Arrays.asList(validNames));
    }

    public NameChecker(@NotNull List<String> validNames) {
        for (String validName : validNames) {
            this.validNames.add(Name.guess(validName));
        }
    }

    public NameChecker(@NotNull Collection<Name> validNames) {
        this.validNames.addAll(validNames);
    }

    public NameChecker(@NotNull Name... validNames) {
        this.validNames.addAll(Lists.newArrayList(validNames));
    }

    public boolean isValid(@NotNull Name name) {
        for (Name validName : validNames) {
            if (name.equals(validName)) {
                return true;
            }
        }
        return false;
    }
}
