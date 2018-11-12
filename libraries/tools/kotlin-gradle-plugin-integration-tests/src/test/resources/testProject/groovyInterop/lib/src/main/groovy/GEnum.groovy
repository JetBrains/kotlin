package genum

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@interface FooEnum {}

enum GEnum {
    FOO("123");

    String value

    GEnum(@FooEnum String value) {
        this.value = value
    }
}