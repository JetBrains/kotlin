package jet.runtime;

/**
 * @author alex.tkachman
 */
public class Intrinsics {
    private Intrinsics() {
    }

    public static String stringPlus(String self, Object other) {
        return ((self == null) ? "null" : self) + ((other == null) ? "null" : other.toString());
    }

    public static Object npe(Object self) {
        if(self == null)
            return throwNpe();
        return self;
    }

    private static Object throwNpe() {
        throw new JetNullPointerException();
    }
}
