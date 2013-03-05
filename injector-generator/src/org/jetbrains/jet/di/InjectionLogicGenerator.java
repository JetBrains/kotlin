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

package org.jetbrains.jet.di;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.Printer;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public abstract class InjectionLogicGenerator {

    public static void generateForFields(@NotNull Printer p, @NotNull Collection<Field> fields) {
        new InjectionLogicGenerator() {
            @Override
            public String prefixForPostConstructorCall(Field field) {
                return "";
            }

            @Override
            public String prefixForSetterCall(Field field) {
                return field.isPublic() ? "this." : "";
            }

            @Override
            public String prefixForInitialization(Field field) {
                return "this.";
            }
        }.generate(p, fields);
    }

    public static void generateForLocalVariables(
            @NotNull final ImportManager importManager,
            @NotNull Printer p,
            @NotNull Collection<Field> fields
    ) {
        new InjectionLogicGenerator() {
            @Override
            public String prefixForPostConstructorCall(Field field) {
                return "";
            }

            @Override
            public String prefixForSetterCall(Field field) {
                return "";
            }

            @Override
            public String prefixForInitialization(Field field) {
                return importManager.render(field.getType()) + " ";
            }
        }.generate(p, fields);
    }

    protected void generate(@NotNull Printer p, @NotNull Collection<Field> fields) {
        // Initialize fields
        for (Field field : fields) {
            //if (!backsParameter.contains(field) || field.isPublic()) {
            p.println(prefixForInitialization(field), field.getName(), " = ", field.getInitialization().renderAsCode(), ";");
            //}
        }
        p.printlnWithNoIndent();

        // Call setters
        for (Field field : fields) {
            for (SetterDependency dependency : field.getDependencies()) {
                String prefix = prefixForSetterCall(field);
                String dependencyName = dependency.getDependency().getName();
                String dependentName = dependency.getDependent().getName();
                p.println(prefix, dependentName, ".", dependency.getSetterName(), "(", dependencyName, ");");
            }
            if (!field.getDependencies().isEmpty()) {
                p.printlnWithNoIndent();
            }
        }

        // call @PostConstruct
        for (Field field : fields) {
            // TODO: type of field may be different from type of object
            List<Method> postConstructMethods = InjectorGeneratorUtil
                    .getPostConstructMethods(InjectorGeneratorUtil.getEffectiveFieldType(field).getClazz());
            for (Method postConstruct : postConstructMethods) {
                p.println(prefixForPostConstructorCall(field), field.getName(), ".", postConstruct.getName(), "();");
            }
            if (postConstructMethods.size() > 0) {
                p.printlnWithNoIndent();
            }
        }
    }

    protected abstract String prefixForInitialization(Field field);

    protected abstract String prefixForSetterCall(Field field);

    protected abstract String prefixForPostConstructorCall(Field field);
}
