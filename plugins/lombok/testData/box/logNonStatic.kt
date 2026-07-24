// ISSUE: KT-86839
// FULL_JDK
// WITH_ADVANCED_LOGGERS

// FILE: JavaLogExample.java

import lombok.AccessLevel;
import lombok.extern.java.Log;


@Log(access = AccessLevel.PUBLIC)
public class JavaLogExample {
    void test() {
        myLog.info("Check @Log from Java");
    }
}

// FILE: JavaSlf4jExample.java

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

@Slf4j(access = AccessLevel.PUBLIC)
public class JavaSlf4jExample {
    void test() {
        myLog.info("Check @Slf4j from Java");
    }
}

// FILE: test.kt

fun box(): String {
    JavaLogExample().apply {
        test()
        myLog.info("Check @Log from Kotlin")
    }

    JavaSlf4jExample().apply {
        test()
        myLog.info("Check @Slf4j from Kotlin")
    }

    return "OK"
}

// FILE: lombok.config
lombok.log.fieldName=myLog
lombok.log.fieldIsStatic=false
