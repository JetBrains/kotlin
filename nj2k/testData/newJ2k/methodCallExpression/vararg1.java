//file
import java.lang.reflect.Constructor;

class X {
    static <T> void foo(Constructor<T> constructor, Object[] args1, Object[] args2) throws Exception {
        constructor.newInstance(args1);
        constructor.newInstance(args1, args2);
    }
}