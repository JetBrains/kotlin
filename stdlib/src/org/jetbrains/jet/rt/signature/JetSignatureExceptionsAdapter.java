package org.jetbrains.jet.rt.signature;

import jet.typeinfo.TypeInfoVariance;

/**
 * @author Stepan Koltsov
 */
public class JetSignatureExceptionsAdapter implements JetSignatureVisitor {
    @Override
    public JetSignatureVisitor visitFormalTypeParameter(String name, TypeInfoVariance variance) {
        throw new IllegalStateException();
    }

    @Override
    public void visitFormalTypeParameterEnd() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitClassBound() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitInterfaceBound() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitSuperclass() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitInterface() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitParameterType() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitReturnType() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitExceptionType() {
        throw new IllegalStateException();
    }

    @Override
    public void visitBaseType(char descriptor, boolean nullable) {
        throw new IllegalStateException();
    }

    @Override
    public void visitTypeVariable(String name, boolean nullable) {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitArrayType(boolean nullable) {
        throw new IllegalStateException();
    }

    @Override
    public void visitClassType(String name, boolean nullable, boolean forceReal) {
        throw new IllegalStateException();
    }

    @Override
    public void visitInnerClassType(String name, boolean nullable) {
        throw new IllegalStateException();
    }

    @Override
    public void visitTypeArgument() {
        throw new IllegalStateException();
    }

    @Override
    public JetSignatureVisitor visitTypeArgument(JetSignatureVariance wildcard) {
        throw new IllegalStateException();
    }

    @Override
    public void visitEnd() {
        throw new IllegalStateException();
    }
}
