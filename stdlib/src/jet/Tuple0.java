package jet;

/**
 * @author alex.tkachman
 */
public class Tuple0 {
    public static final Tuple0 INSTANCE = new Tuple0();

    private Tuple0() {
    }

    @Override
    public String toString() {
        return "()";
    }

    @Override
    public boolean equals(Object o) {
        return o == INSTANCE;
    }

    @Override
    public int hashCode() {
        return 239;
    }
}