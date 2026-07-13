// WITH_STDLIB
// FULL_JDK

import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j

@Log
class LogExample {
    fun test() {
        log.info("Test LogExample")
    }
}

@<!MISSING_DEPENDENCY_CLASS!>Slf4j<!>
class Slf4jExample {
    fun test() {
        <!MISSING_DEPENDENCY_CLASS("org.slf4j.Logger")!>log<!>.<!UNRESOLVED_REFERENCE!>info<!>("Test Slf4jExample")
    }
}

@<!MISSING_DEPENDENCY_CLASS("org.slf4j.Logger")!>Slf4j<!>
class Slf4jExampleWithoutLogUsage
