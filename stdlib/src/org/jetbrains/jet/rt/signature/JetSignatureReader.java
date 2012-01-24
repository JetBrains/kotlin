package org.jetbrains.jet.rt.signature;

import jet.typeinfo.TypeInfoVariance;

/**
 * @author Stepan Koltsov
 * 
 * @see SignatureReader
 */
public class JetSignatureReader {
    
    private final String signature;

    public JetSignatureReader(String signature) {
        this.signature = signature;
    }


    public void accept(final JetSignatureVisitor v) {
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
        } else {
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
                } else if (signature.substring(pos).startsWith("out ")) {
                    variance = TypeInfoVariance.OUT;
                    pos += "out ".length();
                } else {
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
                if (c == 'L' || c == '[' || c == 'T' || c == '?') {
                    pos = parseType(signature, pos, parameterVisitor.visitClassBound());
                }

                while ((c = signature.charAt(pos)) == ':') {
                    ++pos;
                    pos = parseType(signature, pos, parameterVisitor.visitInterfaceBound());
                }
                
                parameterVisitor.visitFormalTypeParameterEnd();
            } while (c != '>');
            ++pos;
        } else {
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
            final String signature,
            int pos,
            final JetSignatureVisitor v)
    {
        if (signature.length() == 0) {
            throw new IllegalStateException();
        }
        
        char c;
        int start, end;
        boolean visited, inner;
        String name;

        boolean nullable = false;
        if (signature.charAt(pos) == '?') {
            nullable = true;
            pos++;
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
                return parseType(signature, pos, v.visitArrayType(nullable));

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
                for (;;) {
                    switch (c = signature.charAt(pos++)) {
                        case '.':
                        case ';':
                            if (!visited) {
                                name = signature.substring(start, pos - 1);
                                if (inner) {
                                    v.visitInnerClassType(name, nullable);
                                } else {
                                    v.visitClassType(name, nullable, forceReal);
                                }
                            }
                            if (c == ';') {
                                v.visitEnd();
                                return pos;
                            }
                            start = pos;
                            visited = false;
                            inner = true;
                            break;

                        case '<':
                            name = signature.substring(start, pos - 1);
                            if (inner) {
                                v.visitInnerClassType(name, nullable);
                            } else {
                                v.visitClassType(name, nullable, forceReal);
                            }
                            visited = true;
                            top: for (;;) {
                                switch (c = signature.charAt(pos)) {
                                    case '>':
                                        break top;
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
            default:
                throw new IllegalStateException();
        }
    }


}
