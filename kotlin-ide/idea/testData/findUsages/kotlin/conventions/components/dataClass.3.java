import pack.A;

import java.util.List;

public class JavaClass4 {
    private interface NestedPrivate extends List<A> {
    }

    public interface NestedPublic extends NestedPrivate {
    }

    public static NestedPublic getNested() {
    }
}
