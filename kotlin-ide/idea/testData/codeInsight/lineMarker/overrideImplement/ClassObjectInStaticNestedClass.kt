interface <lineMarker>TestTrait</lineMarker> {
    fun <lineMarker>test</lineMarker>()
}

class A {
    class B {
        companion object : TestTrait { // TODO: No line marker
            override fun <lineMarker descr="Implements function in 'TestTrait'">test</lineMarker>() {
            }
        }
    }
}