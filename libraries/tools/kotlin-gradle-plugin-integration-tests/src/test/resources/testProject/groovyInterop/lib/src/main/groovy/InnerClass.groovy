package inner

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@interface FooInner {}

class Outer {
    class Inner {
        String name

        Inner(@FooInner String name) {
            this.name = name
        }
    }
}
