// PROBLEM: none
class A(private val someText: String) {
    private <caret>inner class B() : C(someText)
}

abstract class C(private val text: String)