import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class Java8Class {
    public void foo0(Function0<String> r) {
    }

    public void foo1(Function1<Integer, String> r) {
    }

    public void foo2(Function2<Integer, Integer, String> r) {
    }

    public void helper() {
    }

    public void foo() {
        foo0(() -> "42");
        foo0(() -> { return "42"; });
        foo0(() -> {
            helper();
            return "42";
        });

        foo1((i) -> "42");
        foo1(i -> { return "42"; });
        foo1((Integer i) -> {
            helper();
            if (i > 1) {
                return "42";
            }

            return "43";
        });

        foo2((i, j) -> "42");
        foo2((Integer i, Integer j) -> {
            helper();
            return "42";
        });

        Function2<Integer, Integer, String> f = (Integer i, Integer k) -> {
            helper();
            if (i > 1) {
                return "42";
            }

            return "43";
        };

        Function2<Integer, Integer, String> f1 = (Integer i1, Integer k1) -> {
            Function2<Integer, Integer, String> f2 = (Integer i2, Integer k2) -> {
                helper();
                if (i2 > 1) {
                    return "42";
                }

                return "43";
            };
            if (i1 > 1) {
                return f.invoke(i1, k1);
            }
            return f.invoke(i1, k1);
        };

        Runnable runnable = () -> { };

        foo1((Integer i) -> {
            if (i > 1) {
                return "42";
            }

            foo0(() -> {
                if (true) {
                    return "42";
                }
                return "43";
            });

            return "43";
        });
    }
}