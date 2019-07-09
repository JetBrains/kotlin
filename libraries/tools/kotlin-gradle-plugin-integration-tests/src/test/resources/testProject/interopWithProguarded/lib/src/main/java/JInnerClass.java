package iclass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class JInnerClass {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {}

    public class Inner {
        public String foo;

        public Inner(@Foo String foo) {
            this.foo = foo;
        }
    }
}
