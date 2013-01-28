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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class Field {

    public static Field create(boolean isPublic, DiType type, String name, @Nullable Expression init) {
        Field field = new Field(isPublic, type, name);
        if (init != null) {
            field.setInitialization(init);
        }
        return field;
    }

    private final DiType type;
    private final String name;
    private final boolean isPublic;

    @NotNull
    private Expression initialization;

    private final List<SetterDependency> dependencies = Lists.newArrayList();

    Field(boolean isPublic, DiType type, String name) {
        this.isPublic = isPublic;
        this.type = type;
        this.name = name;
        this.initialization = new InstantiateType(type);
    }

    public DiType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return type.getClazz().getSimpleName();
    }

    public String getGetterName() {
        String prefix;
        if (getType().getClazz() == boolean.class || getType().getClazz() == Boolean.class) {
            prefix = "is";
        }
        else {
            prefix = "get";
        }
        return prefix + StringUtil.capitalize(getName());
    }

    @NotNull
    public Expression getInitialization() {
        return initialization;
    }

    public void setInitialization(@NotNull Expression initialization) {
        this.initialization = initialization;
    }

    public List<SetterDependency> getDependencies() {
        return dependencies;
    }

    public boolean isPublic() {
        return isPublic;
    }

    @Override
    public String toString() {
        return getTypeName() + " " + getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Field field = (Field) o;

        if (!name.equals(field.name)) return false;
        if (!type.equals(field.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
