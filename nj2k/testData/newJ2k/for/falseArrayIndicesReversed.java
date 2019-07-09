import java.util.Collection;

public class A {
    void foo(String[] array) {
        for(int i = array.length; i >= 0; i--) {
            System.out.println(i);
        }
    }
}
