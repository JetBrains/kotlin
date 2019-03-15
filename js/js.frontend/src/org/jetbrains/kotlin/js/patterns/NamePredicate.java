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

package org.jetbrains.kotlin.js.patterns;

import com.google.common.collect.Lists;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.name.Name;

import java.util.*;
import java.util.function.Predicate;

public final class NamePredicate implements Predicate<Name> {

    @NotNull
    public static final NamePredicate PRIMITIVE_NUMBERS = new NamePredicate(
            CollectionsKt.map(PrimitiveType.NUMBER_TYPES, (PrimitiveType type) -> type.getTypeName().asString())
    );

    @NotNull
    public static final NamePredicate PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS = new NamePredicate(
            CollectionsKt.mapNotNull(PrimitiveType.NUMBER_TYPES, (PrimitiveType type) ->
                    type != PrimitiveType.LONG ? type.getTypeName().asString() : null
            )
    );

    @NotNull
    public static final NamePredicate STRING = new NamePredicate("String");

    @NotNull
    public static final NamePredicate NUMBER = new NamePredicate("Number");

    @NotNull
    public static final NamePredicate BOOLEAN = new NamePredicate("Boolean");

    @NotNull
    public static final NamePredicate CHAR = new NamePredicate(PrimitiveType.CHAR.getTypeName());

    @NotNull
    public static final NamePredicate LONG = new NamePredicate(PrimitiveType.LONG.getTypeName());

    @NotNull
    private final Set<Name> validNames = new HashSet<>();

    public NamePredicate(@NotNull String... validNames) {
        this(Arrays.asList(validNames));
    }

    private NamePredicate(@NotNull List<String> validNames) {
        for (String validName : validNames) {
            this.validNames.add(Name.guessByFirstCharacter(validName));
        }
    }

    public NamePredicate(@NotNull Collection<Name> validNames) {
        this.validNames.addAll(validNames);
    }

    public NamePredicate(@NotNull Name... validNames) {
        this.validNames.addAll(Lists.newArrayList(validNames));
    }

    @Override
    public boolean test(Name name) {
        return validNames.contains(name);
    }
}
