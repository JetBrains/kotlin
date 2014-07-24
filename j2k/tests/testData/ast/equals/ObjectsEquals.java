//file
import java.util.Objects;

interface I {
}

class C{
    boolean foo1(I i1, I i2) {
        return Objects.equals(i1, i2))
    }

    boolean foo2(I i1, I i2) {
        return !java.util.Objects.equals(i1, i2)
    }
}