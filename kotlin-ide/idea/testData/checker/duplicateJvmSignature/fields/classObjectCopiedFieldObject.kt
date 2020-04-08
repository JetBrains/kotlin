class B {
    companion object <error>A</error> {
    }

    val <error>A</error> = this
}

class C {
    companion <error>object A</error> {
        <error>val A</error> = this
    }

}
