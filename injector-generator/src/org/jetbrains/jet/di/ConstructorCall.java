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

package org.jetbrains.jet.di;

import com.google.common.collect.Lists;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;

/**
* @author abreslav
*/
class ConstructorCall implements Expression {
    private final Constructor<?> constructor;
    private final List<Field> constructorArguments = Lists.newArrayList();

    ConstructorCall(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public List<Field> getConstructorArguments() {
        return constructorArguments;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("new " + constructor.getDeclaringClass().getSimpleName() + "(");
        for (Iterator<Field> iterator = constructorArguments.iterator(); iterator.hasNext(); ) {
            Field argument = iterator.next();
            builder.append(argument.getGetterName() + "()");
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }
}
