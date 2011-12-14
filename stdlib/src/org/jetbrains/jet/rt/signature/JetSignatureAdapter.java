package org.jetbrains.jet.rt.signature;

import jet.typeinfo.TypeInfoVariance;

/**
 * @author Stepan Koltsov
 */
public class JetSignatureAdapter implements JetSignatureVisitor {
    @Override
    public void visitFormalTypeParameter(String name, TypeInfoVariance variance) {
    }

    @Override
    public JetSignatureVisitor visitClassBound() {
        return this;
    }

    @Override
    public JetSignatureVisitor visitInterfaceBound() {
        return this;
    }

    @Override
    public JetSignatureVisitor visitSuperclass() {
        return this;
    }

    @Override
    public JetSignatureVisitor visitInterface() {
        return this;
    }

    @Override
    public JetSignatureVisitor visitParameterType() {
        return this;
    }

    @Override
    public JetSignatureVisitor visitReturnType() {
        return this;
    }

    @Override
    public JetSignatureVisitor visitExceptionType() {
        return this;
    }

    @Override
    public void visitBaseType(char descriptor, boolean nullable) {
    }

    @Override
    public void visitTypeVariable(String name, boolean nullable) {
    }

    @Override
    public JetSignatureVisitor visitArrayType(boolean nullable) {
        return this;
    }

    @Override
    public void visitClassType(String name, boolean nullable) {
    }

    @Override
    public void visitInnerClassType(String name, boolean nullable) {
    }

    @Override
    public void visitTypeArgument() {
    }

    @Override
    public JetSignatureVisitor visitTypeArgument(char wildcard) {
        return this;
    }

    @Override
    public void visitEnd() {
    }
}
