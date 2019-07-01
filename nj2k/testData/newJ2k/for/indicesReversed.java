import java.util.Collection;

public class A {
    void foo(Collection<String> collection) {
        for(int i = collection.size() - 1; i >= 0; i--) {
            System.out.println(i);
        }
    }
}
