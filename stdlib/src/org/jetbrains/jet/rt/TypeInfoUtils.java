package org.jetbrains.jet.rt;

import jet.typeinfo.TypeInfo;
import jet.typeinfo.TypeInfoProjection;

import java.util.List;

/**
 * @author abreslav
 * @author yole
 * @author alex.tkachman
 * @author Stepan Koltsov
 */
class TypeInfoUtils {

    static TypeInfo<?> substitute(TypeInfo<?> typeInfo, TypeInfoProjection[] projections) {
        if (typeInfo instanceof TypeInfoImpl<?>) {
            TypeInfoImpl<?> typeInfoImpl = (TypeInfoImpl<?>) typeInfo;
            return typeInfoImpl.substitute(projections);
        } else if (typeInfo instanceof TypeInfoVar<?>) {
            TypeInfoVar<?> typeInfoVar = (TypeInfoVar<?>) typeInfo;
            return typeInfoVar.substitute(projections);
        } else {
            throw new IllegalStateException();
        }
    }

    static TypeInfo<?> substitute(TypeInfo<?> typeInfo, List<TypeInfo> myVars) {
        if (typeInfo instanceof TypeInfoImpl<?>) {
            TypeInfoImpl<?> typeInfoImpl = (TypeInfoImpl<?>) typeInfo;
            return typeInfoImpl.substitute(myVars);
        } else if (typeInfo instanceof TypeInfoVar<?>) {
            TypeInfoVar<?> typeInfoVar = (TypeInfoVar<?>) typeInfo;
            return typeInfoVar.substitute(myVars);
        } else {
            throw new IllegalStateException();
        }
    }

}
