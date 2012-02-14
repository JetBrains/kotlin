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

package org.jetbrains.jet.rt;

import jet.TypeInfo;
import jet.typeinfo.TypeInfoProjection;
import jet.typeinfo.TypeInfoVariance;

/**
 * @author abreslav
 * @author yole
 * @author alex.tkachman
 * @author Stepan Koltsov
 */
public abstract class TypeInfoProjectionImpl implements TypeInfoProjection {
    private final TypeInfo type;

    public TypeInfoProjectionImpl(TypeInfo typeInfo) {
        this.type = typeInfo;
    }

    @Override
    public final TypeInfo getType() {
        return type;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeInfoProjectionImpl that = (TypeInfoProjectionImpl) o;
        // no need to compare variance as we compared classes already
        return type.equals(that.type);
    }

    @Override
    public final int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (getVariance().hashCode());
        return result;
    }

    @Override
    public final String toString() {
        return (getVariance() == TypeInfoVariance.INVARIANT ? "" : getVariance().toString() + " ") + type;
    }
}
