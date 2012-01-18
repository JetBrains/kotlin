package org.jetbrains.jet.rt.signature;

import jet.typeinfo.TypeInfoVariance;

/**
 * @author Stepan Koltsov
 *
 * @see SignatureWriter
 */
public class JetSignatureWriter implements JetSignatureVisitor {

    /**
     * Buffer used to construct the signature.
     */
    private final StringBuffer buf = new StringBuffer();

    /**
     * Indicates if the signature contains formal type parameters.
     */
    private boolean hasFormals;

    /**
     * Indicates if the signature contains method parameter types.
     */
    private boolean hasParameters;

    /**
     * Stack used to keep track of class types that have arguments. Each element
     * of this stack is a boolean encoded in one bit. The top of the stack is
     * the lowest order bit. Pushing false = *2, pushing true = *2+1, popping =
     * /2.
     */
    private int argumentStack;

    /**
     * Constructs a new {@link SignatureWriter} object.
     */
    public JetSignatureWriter() {
    }

    // ------------------------------------------------------------------------
    // Implementation of the SignatureVisitor interface
    // ------------------------------------------------------------------------

    @Override
    public JetSignatureVisitor visitFormalTypeParameter(final String name, TypeInfoVariance variance) {
        if (!hasFormals) {
            hasFormals = true;
            buf.append('<');
        }
        switch (variance) {
            case OUT:
                buf.append("out ");
                break;
            case IN:
                buf.append("in ");
                break;
            case INVARIANT:
                break;
            default:
                throw new IllegalStateException();
        }
        buf.append(name);
        buf.append(':');
        return this;
    }

    @Override
    public void visitFormalTypeParameterEnd() {
    }

    @Override
    public JetSignatureWriter visitClassBound() {
        return this;
    }

    @Override
    public JetSignatureWriter visitInterfaceBound() {
        buf.append(':');
        return this;
    }

    @Override
    public JetSignatureWriter visitSuperclass() {
        endFormals();
        return this;
    }

    @Override
    public JetSignatureWriter visitInterface() {
        return this;
    }

    @Override
    public JetSignatureWriter visitParameterType() {
        endFormals();
        if (!hasParameters) {
            hasParameters = true;
            buf.append('(');
        }
        return this;
    }

    @Override
    public JetSignatureWriter visitReturnType() {
        endFormals();
        if (!hasParameters) {
            buf.append('(');
        }
        buf.append(')');
        return this;
    }

    @Override
    public JetSignatureWriter visitExceptionType() {
        buf.append('^');
        return this;
    }

    private void visitNullabe(boolean nullable) {
        if (nullable) {
            buf.append('?');
        }
    }

    @Override
    public void visitBaseType(final char descriptor, boolean nullable) {
        visitNullabe(nullable);
        buf.append(descriptor);
    }

    @Override
    public void visitTypeVariable(final String name, boolean nullable) {
        visitNullabe(nullable);
        buf.append('T');
        buf.append(name);
        buf.append(';');
    }

    @Override
    public JetSignatureWriter visitArrayType(boolean nullable) {
        visitNullabe(nullable);
        buf.append('[');
        return this;
    }

    @Override
    public void visitClassType(final String name, boolean nullable) {
        visitNullabe(nullable);
        buf.append('L');
        buf.append(name);
        argumentStack *= 2;
    }

    @Override
    public void visitInnerClassType(final String name, boolean nullable) {
        endArguments();
        visitNullabe(nullable);
        buf.append('.');
        buf.append(name);
        argumentStack *= 2;
    }

    @Override
    public void visitTypeArgument() {
        if (argumentStack % 2 == 0) {
            ++argumentStack;
            buf.append('<');
        }
        buf.append('*');
    }

    @Override
    public JetSignatureWriter visitTypeArgument(JetSignatureVariance variance) {
        if (argumentStack % 2 == 0) {
            ++argumentStack;
            buf.append('<');
        }
        if (variance.getC() != '=') {
            buf.append(variance.getC());
        }
        return this;
    }

    @Override
    public void visitEnd() {
        endArguments();
        buf.append(';');
    }

    public String toString() {
        return buf.toString();
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Ends the formal type parameters section of the signature.
     */
    private void endFormals() {
        if (hasFormals) {
            hasFormals = false;
            buf.append('>');
        }
    }

    /**
     * Ends the type arguments of a class or inner class type.
     */
    private void endArguments() {
        if (argumentStack % 2 != 0) {
            buf.append('>');
        }
        argumentStack /= 2;
    }
}
