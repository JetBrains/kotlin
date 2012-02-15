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

import jet.JetObject;
import jet.TypeInfo;
import jet.typeinfo.TypeInfoProjection;

import java.util.List;

/**
 * @author abreslav
 * @author yole
 * @author alex.tkachman
 * @author Stepan Koltsov
 */
class TypeInfoVar<T> extends TypeInfo<T> {
    final int varIndex;
    final Signature signature;
    final boolean nullable;

    public TypeInfoVar(Signature signature, Integer varIndex) {
        this.signature = signature;
        this.varIndex = varIndex;
        nullable = false;
    }

    public TypeInfoVar(Signature signature, boolean nullable, int varIndex) {
        this.signature = signature;
        this.nullable = nullable;
        this.varIndex = varIndex;
    }

    @Override
    public Object[] newArray(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<T> getJavaClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getClassObject() {
        throw new UnsupportedOperationException("Abstract TypeInfo");
    }

    @Override
    public boolean isInstance(Object obj) {
        throw new UnsupportedOperationException("Abstract TypeInfo");
    }

    @Override
    public int getProjectionCount() {
        return 0;
    }

    @Override
    public TypeInfoProjection getProjection(int index) {
        throw new UnsupportedOperationException("Abstract TypeInfo");
    }

    @Override
    public TypeInfo getArgumentType(Class klass, int index) {
        throw new UnsupportedOperationException("Abstract TypeInfo");
    }

    TypeInfo substitute(List<TypeInfo> myVars) {
        return myVars.get(varIndex);
    }

    TypeInfo substitute(TypeInfoProjection[] projections) {
        return projections[varIndex].getType();
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        throw new UnsupportedOperationException("Abstract TypeInfo");
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    @Override
    public String toString() {
        return "T:" + signature.klazz.getName() + ":" + varIndex;
    }
}
