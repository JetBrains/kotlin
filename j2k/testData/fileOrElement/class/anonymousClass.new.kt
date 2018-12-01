package test

import javaApi.Listener

class Test {
    private val listener = Listener { visibility -> val a = visibility and 1 }
}