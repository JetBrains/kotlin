import java.util.Arrays;
import java.util.List;

public class Foo {
    public void test() {
        List<String> list = Arrays.asList("a", "b");
        Object[] array1 = list.toArray();
        Object[] array2 = list.toArray(new String[list.size()]);
    }
}