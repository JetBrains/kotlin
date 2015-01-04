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

package org.jetbrains.kotlin.js.facade;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class MainCallParameters {
    @NotNull
    public static MainCallParameters noCall() {
        return new MainCallParameters() {

            @NotNull
            @Override
            public List<String> arguments() {
                throw new UnsupportedOperationException("#arguments");
            }

            @Override
            public boolean shouldBeGenerated() {
                return false;
            }
        };
    }


    @NotNull
    public static MainCallParameters mainWithoutArguments() {
        return new MainCallParameters() {

            @NotNull
            @Override
            public List<String> arguments() {
                return Collections.emptyList();
            }

            @Override
            public boolean shouldBeGenerated() {
                return true;
            }
        };
    }

    @NotNull
    public static MainCallParameters mainWithArguments(@NotNull final List<String> parameters) {
        return new MainCallParameters() {

            @NotNull
            @Override
            public List<String> arguments() {
                return parameters;
            }

            @Override
            public boolean shouldBeGenerated() {
                return true;
            }
        };
    }

    public abstract boolean shouldBeGenerated();

    @NotNull
    public abstract List<String> arguments();
}
