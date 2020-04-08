interface I {
    fun ov_foo()
}

class A : I {
    override fun ov<caret>
}

// ABSENT: override
// EXIST: { itemText: "override fun ov_foo() {...}", allLookupStrings: "ov_foo", tailText: null, typeText: "I", attributes: "bold" }
