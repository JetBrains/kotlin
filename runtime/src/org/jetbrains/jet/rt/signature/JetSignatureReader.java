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

package org.jetbrains.jet.rt.signature;

import jet.typeinfo.TypeInfoVariance;

/**
 * @see SignatureReader
 */
public class JetSignatureReader {
    
    private final String signature;

    public JetSignatureReader(String signature) {
        this.signature = signature;
    }


    public void accept(JetSignatureVisitor v) {
        String signature = this.signature;
        int len = signature.length();
        int pos = acceptFormalTypeParameters(v);

        if (signature.charAt(pos) == '(') {
            pos++;
            while (signature.charAt(pos) != ')') {
                pos = parseType(signature, pos, v.visitParameterType());
            }
            pos = parseType(signature, pos + 1, v.visitReturnType());
            while (pos < len) {
                pos = parseType(signature, pos + 1, v.visitExceptionType());
            }
        }
        else {
            pos = parseType(signature, pos, v.visitSuperclass());
            while (pos < len) {
                pos = parseType(signature, pos, v.visitInterface());
            }
        }
        
        if (pos != signature.length()) {
            throw new IllegalStateException();
        }
    }

    public int acceptFormalTypeParameters(JetSignatureVisitor v) {
        int pos;
        char c;
        if (signature.length() > 0 && signature.charAt(0) == '<') {
            pos = 1;
            do {
                TypeInfoVariance variance;
                boolean reified = true;
                
                if (signature.substring(pos).startsWith("erased ")) {
                    reified = false;
                    pos += "erased ".length();
                }
                if (signature.substring(pos).startsWith("in ")) {
                    variance = TypeInfoVariance.IN;
                    pos += "in ".length();
                }
                else if (signature.substring(pos).startsWith("out ")) {
                    variance = TypeInfoVariance.OUT;
                    pos += "out ".length();
                }
                else {
                    variance = TypeInfoVariance.INVARIANT;
                    pos += "".length();
                }
                int end = signature.indexOf(':', pos);
                if (end < 0) {
                    throw new IllegalStateException();
                }
                String typeParameterName = signature.substring(pos, end);
                if (typeParameterName.isEmpty()) {
                    throw new IllegalStateException("incorrect signature: " + signature);
                }
                JetSignatureVisitor parameterVisitor = v.visitFormalTypeParameter(typeParameterName, variance, reified);
                pos = end + 1;

                c = signature.charAt(pos);
                if (c == 'L' || c == 'M' || c == '[' || c == 'T' || c == '?') {
                    pos = parseType(signature, pos, parameterVisitor.visitClassBound());
                }

                while ((c = signature.charAt(pos)) == ':') {
                    ++pos;
                    pos = parseType(signature, pos, parameterVisitor.visitInterfaceBound());
                }
                
                parameterVisitor.visitFormalTypeParameterEnd();
            } while (c != '>');
            ++pos;
        }
        else {
            pos = 0;
        }
        return pos;
    }
    
    public void acceptFormalTypeParametersOnly(JetSignatureVisitor v) {
        int r = acceptFormalTypeParameters(v);
        if (r != signature.length()) {
            throw new IllegalStateException();
        }
    }

    public int acceptType(JetSignatureVisitor v) {
        return parseType(this.signature, 0, v);
    }

    public void acceptTypeOnly(JetSignatureVisitor v) {
        int r = acceptType(v);
        if (r != signature.length()) {
            throw new IllegalStateException();
        }
    }


    private static int parseType(
            String signature,
            int pos,
            JetSignatureVisitor v)
    {
        if (signature.length() == 0) {
            throw new IllegalStateException();
        }
        
        char c;
        int start;
        int end;
        boolean visited;
        boolean inner;

        boolean nullable;
        if (signature.charAt(pos) == '?') {
            nullable = true;
            pos++;
        }
        else {
            nullable = false;
        }

        switch (c = signature.charAt(pos++)) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'F':
            case 'J':
            case 'D':
            case 'V':
                v.visitBaseType(c, nullable);
                return pos;

            case '[':
                switch (c = signature.charAt(pos)) {
                    case '+':
                    case '-':
                        return parseType(signature, pos + 1, v.visitArrayType(nullable, JetSignatureVariance.parseVariance(c)));
                    default:
                        return parseType(signature, pos, v.visitArrayType(nullable, JetSignatureVariance.INVARIANT));
                }

            case 'T':
                end = signature.indexOf(';', pos);
                v.visitTypeVariable(signature.substring(pos, end), nullable);
                return end + 1;

            case 'L':
            case 'M':
                boolean forceReal = signature.charAt(pos - 1) == 'M';
                start = pos;
                visited = false;
                inner = false;
                while (true) {
                    switch (c = signature.charAt(pos++)) {
                        case '.':
                        case ';':
                            if (!visited) {
                                parseTypeConstructor(signature, v, start, pos, inner, nullable, forceReal);
                            }
                            if (c == ';') {
                                v.visitEnd();
                                return pos;
                            }
                            visited = false;
                            inner = true;
                            break;

                        case '<':
                            parseTypeConstructor(signature, v, start, pos, inner, nullable, forceReal);
                            visited = true;
                            pos = parseTypeArguments(signature, pos, v);

                        default:
                            break;
                    }
                }
            default:
                throw new IllegalStateException();
        }
    }

    private static void parseTypeConstructor(
            String signature,
            JetSignatureVisitor v,
            int start,
            int pos,
            boolean inner,
            boolean nullable,
            boolean forceReal
    ) {
        String name = signature.substring(start, pos - 1);
        if (inner) {
            v.visitInnerClassType(name, nullable, forceReal);
        }
        else {
            v.visitClassType(name, nullable, forceReal);
        }
    }

    private static int parseTypeArguments(String signature, int pos, JetSignatureVisitor v) {
        char c;
        while (true) {
            switch (c = signature.charAt(pos)) {
                case '>':
                    return pos;
                case '*':
                    ++pos;
                    v.visitTypeArgument();
                    break;
                case '+':
                case '-':
                    pos = parseType(signature,
                                    pos + 1,
                                    v.visitTypeArgument(JetSignatureVariance.parseVariance(c)));
                    break;
                default:
                    pos = parseType(signature,
                                    pos,
                                    v.visitTypeArgument(JetSignatureVariance.INVARIANT));
                    break;
            }
        }
    }
}
