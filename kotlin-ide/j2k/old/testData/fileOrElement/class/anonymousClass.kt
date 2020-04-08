package test

import javaApi.*

class Test {
    private val listener = Listener { visibility -> val a = visibility and 1 }
}