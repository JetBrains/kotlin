// ISSUE: KT-86839
// FULL_JDK
// WITH_ADVANCED_LOGGERS

// FILE: JavaLogExample.java

import lombok.AccessLevel;
import lombok.extern.java.Log;

@Log(access = AccessLevel.PUBLIC)
public class JavaLogExample {
    static void test() {
        log.info("Check @Log from Java");
    }
}

// FILE: JavaSlf4jExample.java

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

@Slf4j(access = AccessLevel.PUBLIC)
public class JavaSlf4jExample {
    static void test() {
        log.info("Check @Slf4j from Java");
    }
}

// FILE: test.kt

fun box(): String {
    JavaLogExample.test()
    JavaLogExample.log.info("Check @Log from Kotlin")

    JavaSlf4jExample.test()
    JavaSlf4jExample.log.info("Check @Slf4j from Kotlin")

    return "OK"
}
