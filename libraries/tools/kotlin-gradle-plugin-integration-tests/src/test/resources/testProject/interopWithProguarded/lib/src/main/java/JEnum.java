package jenum;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public enum JEnum {
    OK("123");

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Foo {}

    JEnum(@Foo String foo) {}
}
