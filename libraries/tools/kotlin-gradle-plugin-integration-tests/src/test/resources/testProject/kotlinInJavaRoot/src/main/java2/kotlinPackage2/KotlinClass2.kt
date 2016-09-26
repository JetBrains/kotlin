package kotlinPackage2

import javaPackage.*
import javaPackage2.*

public class KotlinClass2() {
    fun foo() {}
}

fun main() {
    JavaClass().foo()
    JavaClass2().foo()
}

