class C {
    fun doSomething() {
        val it = getSomeObjects().iterator()
        while (it.hasNext()) {
            println("text = " + it.next())
        }
    }

    private fun <caret>getSomeObjects(): Collection<Any> {
        val text = "hello"
        return getSomeObjects(text)
    }

    private fun getSomeObjects(text: String): Collection<Any> {
        val list = arrayListOf<Any>()
        list.add(text)
        return list
    }
}