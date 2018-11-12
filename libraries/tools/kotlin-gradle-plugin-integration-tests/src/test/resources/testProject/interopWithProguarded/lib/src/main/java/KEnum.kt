package kenum

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

enum class KEnum(@Foo val foo: String) {
    OK("123");

    @Retention(RetentionPolicy.RUNTIME)
    annotation class Foo {}
}
