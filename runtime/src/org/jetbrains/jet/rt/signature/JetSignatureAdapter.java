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

public class JetSignatureAdapter implements JetSignatureVisitor {
    @Override
    public JetSignatureVisitor visitFormalTypeParameter(String name, TypeInfoVariance variance, boolean reified) {
        return this;
    }

    @Override
    public void visitFormalTypeParameterEnd() {
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
    public JetSignatureVisitor visitArrayType(boolean nullable, JetSignatureVariance wildcard) {
        return this;
    }

    @Override
    public void visitClassType(String name, boolean nullable, boolean forceReal) {
    }

    @Override
    public void visitInnerClassType(String name, boolean nullable, boolean forceReal) {
    }

    @Override
    public void visitTypeArgument() {
    }

    @Override
    public JetSignatureVisitor visitTypeArgument(JetSignatureVariance wildcard) {
        return this;
    }

    @Override
    public void visitEnd() {
    }
}
