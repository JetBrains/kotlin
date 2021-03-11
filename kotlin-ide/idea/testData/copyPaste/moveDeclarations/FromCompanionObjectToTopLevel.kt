package source

class X {
    companion object {
        <selection>
        fun foo() {
            other()
            bar++
        }

        var bar = 1
        </selection>

        fun other() {
            foo()
        }
    }

    fun f() {
        bar++
    }
}

<caret>
