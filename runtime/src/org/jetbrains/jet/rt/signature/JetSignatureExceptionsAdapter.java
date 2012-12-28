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

package org.jetbrains.jet.rt.signature;

import jet.typeinfo.TypeInfoVariance;

public class JetSignatureExceptionsAdapter implements JetSignatureVisitor {
    @Override
    public JetSignatureVisitor visitFormalTypeParameter(String name, TypeInfoVariance variance, boolean reified) {
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
    public JetSignatureVisitor visitArrayType(boolean nullable, JetSignatureVariance wildcard) {
        throw new IllegalStateException();
    }

    @Override
    public void visitClassType(String name, boolean nullable, boolean forceReal) {
        throw new IllegalStateException();
    }

    @Override
    public void visitInnerClassType(String name, boolean nullable, boolean forceReal) {
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
