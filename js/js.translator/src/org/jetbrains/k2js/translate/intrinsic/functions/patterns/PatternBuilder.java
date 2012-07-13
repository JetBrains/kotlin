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
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class PatternBuilder {

    public static final NameChecker JET = new NameChecker("jet");

    private PatternBuilder() {
    }

    @NotNull
    public static Pattern pattern(@NotNull NameChecker checker, @NotNull String stringWithPattern) {
        List<NameChecker> checkers = Lists.newArrayList(checker);
        checkers.addAll(parseStringAsCheckerList(stringWithPattern));
        return pattern(checkers);
    }

    @NotNull
    public static Pattern pattern(@NotNull String string) {
        List<NameChecker> checkers = parseStringAsCheckerList(string);
        return pattern(checkers);
    }

    @NotNull
    private static List<NameChecker> parseStringAsCheckerList(@NotNull String stringWithPattern) {
        String[] subPatterns = stringWithPattern.split("\\.");
        List<NameChecker> checkers = Lists.newArrayList();
        for (String subPattern : subPatterns) {
            String[] validNames = subPattern.split("\\|");
            checkers.add(new NameChecker(validNames));
        }
        return checkers;
    }

    @NotNull
    private static Pattern pattern(@NotNull List<NameChecker> checkers) {
        final List<NameChecker> checkersWithPrefixChecker = Lists.newArrayList(JET);
        checkersWithPrefixChecker.addAll(checkers);
        return new Pattern() {
            @Override
            public boolean apply(@NotNull FunctionDescriptor descriptor) {
                //TODO: no need to wrap if we check beforehand
                try {
                    return doApply(descriptor);
                }
                catch (IllegalArgumentException e) {
                    return false;
                }
            }

            private boolean doApply(@NotNull FunctionDescriptor descriptor) {
                List<Name> nameParts = DescriptorUtils.getFQName(descriptor).pathSegments();
                if (nameParts.size() != checkersWithPrefixChecker.size()) {
                    return false;
                }
                if (!allNamePartsValid(nameParts)) return false;
                return true;
            }

            private boolean allNamePartsValid(@NotNull List<Name> nameParts) {
                for (int i = 0; i < nameParts.size(); ++i) {
                    Name namePart = nameParts.get(i);
                    NameChecker correspondingPredicate = checkersWithPrefixChecker.get(i);
                    if (!correspondingPredicate.isValid(namePart)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    @NotNull
    public static Pattern pattern(@NotNull final NameChecker... checkers) {
        return pattern(Arrays.asList(checkers));
    }

    @NotNull
    public static Pattern any(@NotNull final Pattern... patterns) {
        return new Pattern() {
            @Override
            public boolean apply(@NotNull FunctionDescriptor descriptor) {
                for (Pattern pattern : patterns) {
                    if (pattern.apply(descriptor)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @NotNull
    public static Pattern all(@NotNull final Pattern... patterns) {
        return new Pattern() {
            @Override
            public boolean apply(@NotNull FunctionDescriptor descriptor) {
                for (Pattern pattern : patterns) {
                    if (!pattern.apply(descriptor)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
}
