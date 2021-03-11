import java.util.ArrayList;
import java.util.Collection;

class C<T> {
    void foo1(Collection<? extends T> src) {
        T t = src.iterator().next();
    }

    void foo2(ArrayList<? extends T> src) {
        T t = src.iterator().next();
    }

    void foo3(Collection<? super T> dst, T t) {
        dst.add(t)
    }

    int foo4(Comparable<? super T> comparable, T t) {
        return comparable.compareTo(t);
    }

    void foo5(Collection<?> w) {
    }
}