import java.util.Collection;

public class A {
    void foo(String[] array) {
        for(int i = array.length - 1; i >= 0; i--) {
            System.out.println(i);
        }
    }
}
