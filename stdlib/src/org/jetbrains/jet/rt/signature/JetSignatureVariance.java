package org.jetbrains.jet.rt.signature;

/**
 * @author Stepan Koltsov
 */
public enum JetSignatureVariance {
    INVARIANT('='),
    IN('-'),
    OUT('+'),
    ;
    
    private final char c;

    private JetSignatureVariance(char c) {
        this.c = c;
    }

    public char getC() {
        return c;
    }
    
    public static JetSignatureVariance parseVariance(char c) {
        switch (c) {
            case '=': return INVARIANT;
            case '+': return OUT;
            case '-': return IN;
            default: throw new IllegalStateException();
        }
    }
}
