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

package org.jetbrains.jet.di;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public class ParameterExpression implements Expression {
    private final Parameter parameter;

    public ParameterExpression(Parameter parameter) {
        this.parameter = parameter;
    }

    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public String toString() {
        return "parameter<" + parameter.getName() + ">";
    }

    @NotNull
    @Override
    public String renderAsCode() {
        return parameter.getName();
    }

    @NotNull
    @Override
    public Collection<DiType> getTypesToImport() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public DiType getType() {
        return parameter.getType();
    }
}
