package org.jetbrains.jet.rt;

import jet.JetObject;
import jet.typeinfo.TypeInfo;
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
