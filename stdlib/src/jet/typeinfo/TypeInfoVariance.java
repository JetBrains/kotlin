package jet.typeinfo;

/**
 * @author alex.tkachman
 */
public enum TypeInfoVariance {
    INVARIANT("") ,
    IN_VARIANCE("in"),
    OUT_VARIANCE("out");

    private final String label;

    TypeInfoVariance(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
