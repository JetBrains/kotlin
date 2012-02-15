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
import jet.typeinfo.TypeInfoVariance;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * @author abreslav
 * @author yole
 * @author alex.tkachman
 * @author Stepan Koltsov
 */
public class TypeInfoImpl<T> extends TypeInfo<T> implements TypeInfoProjection {
    private final static TypeInfoProjection[] EMPTY = new TypeInfoProjection[0];

    private TypeInfo<?> typeInfo;
    public final Signature signature;
    private final boolean nullable;
    private final TypeInfoProjection[] projections;

    public TypeInfoImpl(Class<T> theClass, boolean nullable) {
        this(theClass, nullable, EMPTY);
    }

    public TypeInfoImpl(Class<T> theClass, boolean nullable, TypeInfoProjection[] projections) {
        this.signature = TypeInfoParser.parse(theClass);
        this.nullable = nullable;
        this.projections = projections;
        if(signature.variables.size() != projections.length)
            throw new IllegalStateException("Wrong signature " + theClass.getName());
    }

    public final TypeInfoProjection getProjection(int index) {
        return projections[index];
    }

    @Override
    public Object[] newArray(int length) {
        return (Object[]) Array.newInstance(signature.klazz, length);
    }

    @Override
    public Class<T> getJavaClass() {
        return signature.klazz;
    }

    public final Object getClassObject() {
        try {
            final Class implClass = signature.klazz.getClassLoader().loadClass(signature.klazz.getCanonicalName());
            final Field classobj = implClass.getField("$classobj");
            return classobj.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    TypeInfo getSuperTypeInfo(Class klass) {
        return signature.superSignatures.get(klass);
    }

    public final TypeInfo getArgumentType(Class klass, int index) {
        if(klass == this.signature.klazz)
            return projections[index].getType();
        else {
            return TypeInfoUtils.substitute(getSuperTypeInfo(klass), projections).getArgumentType(klass, index);
        }
    }

    TypeInfo substitute(final List<TypeInfo> myVars) {
        if(projections.length == 0)
            return new TypeInfoImpl(this.signature.klazz, nullable, EMPTY);
        else {
            TypeInfoProjection [] proj = new TypeInfoProjection[projections.length];
            for(int i = 0; i != proj.length; ++i) {
                final int finalI = i;
                final TypeInfo substitute = TypeInfoUtils.substitute(projections[finalI].getType(), myVars);
                proj[i] = new TypeInfoProjection(){

                    @Override
                    public TypeInfoVariance getVariance() {
                        return projections[finalI].getVariance();
                    }

                    @Override
                    public TypeInfo getType() {
                        return substitute;
                    }

                    @Override
                    public String toString() {
                        return getVariance().toString() + " " + substitute;
                    }
                };
            }
            return new TypeInfoImpl(this.signature.klazz, nullable, proj);
        }
    }

    TypeInfo substitute(TypeInfoProjection[] prj) {
        if(projections.length == 0)
            return new TypeInfoImpl(signature.klazz, nullable, EMPTY);
        else {
            TypeInfoProjection [] proj = new TypeInfoProjection[projections.length];
            for(int i = 0; i != proj.length; ++i) {
                final int finalI = i;
                final TypeInfo substitute = TypeInfoUtils.substitute(projections[finalI].getType(), prj);
                proj[i] = new TypeInfoProjection(){

                    @Override
                    public TypeInfoVariance getVariance() {
                        return projections[finalI].getVariance();
                    }

                    @Override
                    public TypeInfo getType() {
                        return substitute;
                    }

                    @Override
                    public String toString() {
                        return getVariance().toString() + " " + substitute;
                    }
                };
            }
            return new TypeInfoImpl(signature.klazz, nullable, proj);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeInfoImpl typeInfo = (TypeInfoImpl) o;

        if (!signature.klazz.equals(typeInfo.signature.klazz)) return false;
        if (nullable != typeInfo.nullable) return false;
        if (!Arrays.equals(projections, typeInfo.projections)) return false;

        return true;
    }

//        @NotNull
    @Override
    public TypeInfoVariance getVariance() {
        return TypeInfoVariance.INVARIANT;
    }

//        @NotNull
    @Override
    public TypeInfo getType() {
        return this;
    }

    @Override
    public final int hashCode() {
        return 31 * signature.klazz.hashCode() + Arrays.hashCode(projections);
    }

    public final boolean isInstance(Object obj) {
        if (obj == null) return nullable;

        if (obj instanceof JetObject) {
            return ((TypeInfoImpl)((JetObject) obj).getTypeInfo()).isSubtypeOf(this);
        }

        return signature.klazz.isAssignableFrom(obj.getClass());  // TODO
    }

    @Override
    public int getProjectionCount() {
        return projections.length;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder().append(signature.klazz.getName());
        if (projections.length != 0) {
            sb.append("<");
            for (int i = 0; i != projections.length - 1; ++i) {
                sb.append(projections[i].toString()).append(",");
            }
            sb.append(projections[projections.length - 1].toString()).append(">");
        }
        if (nullable)
            sb.append("?");
        return sb.toString();
    }

    @Override
    public final TypeInfo<?> getTypeInfo() {
        if (typeInfo == null) {
            // TODO: Implementation must be lazy, otherwise the result would be of an infinite size
            throw new UnsupportedOperationException(); // TODO
        }
        return typeInfo;
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public final boolean isSubtypeOf(TypeInfoImpl<?> superType) {
        if (nullable && !superType.nullable) {
            return false;
        }
        if (!superType.signature.klazz.isAssignableFrom(signature.klazz)) {
            return false;
        }
        if (superType.projections == null || superType.projections.length != projections.length) {
            throw new IllegalArgumentException("inconsistent type info for the same class");
        }
        for (int i = 0; i < projections.length; i++) {
            // TODO handle variance here
            if (!projections[i].getType().equals(superType.projections[i].getType())) {
                return false;
            }
        }
        return true;
    }

}
