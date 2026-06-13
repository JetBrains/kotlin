// ISSUE: KT-86839
// FULL_JDK

// FILE: JavaLogExamplePrivate.java

import lombok.AccessLevel;
import lombok.extern.java.Log;

@Log(access = AccessLevel.PRIVATE)
public class JavaLogExamplePrivate

// FILE: pack/JavaLogExampleProtected.java

package pack;

import lombok.AccessLevel;
import lombok.extern.java.Log;

@Log(access = AccessLevel.PROTECTED)
public class JavaLogExampleProtected

// FILE: test.kt

import pack.JavaLogExampleProtected

class KotlinLogExample : JavaLogExampleProtected() {
    fun test() {
        log.info("Access to visible protected log field")
    }
}

fun test() {
    JavaLogExamplePrivate.<!INVISIBLE_REFERENCE!>log<!>.info("Access to invisible private log field")
    JavaLogExampleProtected.<!INVISIBLE_REFERENCE!>log<!>.info("Access to invisible protected log field")
}
