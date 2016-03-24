import java.util.Arrays;

public class Foo {
    public Object[] m() {
        return Arrays.asList("a", "b").toArray();
    }
}