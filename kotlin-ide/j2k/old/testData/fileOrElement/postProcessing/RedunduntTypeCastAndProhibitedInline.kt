object C {

    fun consume1(c: C) {

    }

    fun consume2(c: C) {

    }

    fun foo(cList: List<C>) {
        val iter = cList.iterator()
        while (iter.hasNext()) {
            val c = iter.next()
            consume1(c)
            consume2(c)
        }
    }
}