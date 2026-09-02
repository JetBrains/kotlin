import lombok.extern.java.Log
import lombok.extern.slf4j.Slf4j
import lombok.NoArgsConstructor

@<!MISSING_DEPENDENCY_CLASS("java.util.logging.Logger"), MISSING_DEPENDENCY_CLASS("kotlin.jvm.JvmStatic")!>Log<!>
class LogExample {
    fun test() {
        <!MISSING_DEPENDENCY_CLASS("java.util.logging.Logger")!>log<!>.<!UNRESOLVED_REFERENCE!>info<!>("Test LogExample")
    }
}

@<!MISSING_DEPENDENCY_CLASS("org.slf4j.Logger"), MISSING_DEPENDENCY_CLASS("kotlin.jvm.JvmStatic")!>Slf4j<!>
class Slf4jExample {
    fun test() {
        <!MISSING_DEPENDENCY_CLASS("org.slf4j.Logger")!>log<!>.<!UNRESOLVED_REFERENCE!>info<!>("Test Slf4jExample")
    }
}

@<!MISSING_DEPENDENCY_CLASS("kotlin.jvm.JvmStatic")!>NoArgsConstructor<!>(staticName = "make")
class NoArgsConstructorExample(var x: Int) {
    fun test() {
        make()
    }
}
