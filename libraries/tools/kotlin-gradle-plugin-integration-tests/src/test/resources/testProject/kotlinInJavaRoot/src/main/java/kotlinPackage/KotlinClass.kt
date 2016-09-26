package kotlinPackage

import javaPackage.*
import javaPackage2.*

public class KotlinClass() {
    fun foo() {}
}

fun main() {
    JavaClass().foo()
    JavaClass2().foo()
}

