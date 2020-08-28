// RUNTIME_WITH_FULL_JDK

import java.util.List;

public class TestLambda<T> {
    public static class Box<Q> {

        private Q value;
        public Q unbox() {
            return value;
        }

        public Box(Q q) {
            value = q;
        }
    }

    public String toStringAllBox(List<Box<T>> list) {
        return list.stream().map(Box<T>::unbox).map(T::toString).reduce((s1,s2) -> s1 + ", " + s2 ).orElse("");
    }
}