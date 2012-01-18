package jet.typeinfo;

import org.jetbrains.jet.rt.signature.JetSignatureVariance;

/**
 * @author alex.tkachman
 */
public enum TypeInfoVariance {
    INVARIANT("", JetSignatureVariance.INVARIANT) ,
    IN("in", JetSignatureVariance.IN),
    OUT("out", JetSignatureVariance.OUT);

    private final String label;
    private final JetSignatureVariance variance;

    TypeInfoVariance(String label, JetSignatureVariance variance) {
        this.label = label;
        this.variance = variance;
    }

    @Override
    public String toString() {
        return label;
    }
}
