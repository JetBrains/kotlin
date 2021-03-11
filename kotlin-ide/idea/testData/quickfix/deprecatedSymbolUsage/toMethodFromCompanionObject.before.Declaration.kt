package dependency

class C {
    @Deprecated("", ReplaceWith("newFun(this)"))
    fun oldFun() {}

    companion object {
        fun newFun(c: C) {}
    }
}

