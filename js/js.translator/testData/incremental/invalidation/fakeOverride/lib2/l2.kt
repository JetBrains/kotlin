class ClassB: ClassA() {
    override fun test1() = "ClassB::test1".castTo<String>()
    override fun test2() = 1.castTo<String>()
}
