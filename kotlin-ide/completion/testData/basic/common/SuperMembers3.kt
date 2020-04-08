open class A<T> {
    open fun xxx_foo(p1: T, vararg p2: String) {}
    open fun xxx_bar(p1: Int, p2: String) {}
}

open class B<T> : A<T> {
    open val xxx_val: Int = 0
}

class C : B<String>() {
    override fun xxx_foo(p1: String, vararg p2: String) {
        super.xxx_<caret>
    }
}

// WITH_ORDER
// EXIST: { lookupString: "xxx_foo", itemText: "xxx_foo", tailText: "(p1, *p2)", typeText: "Unit", attributes: "" }
// EXIST: { lookupString: "xxx_foo", itemText: "xxx_foo", tailText: "(p1: String, vararg p2: String)", typeText: "Unit", attributes: "" }
// EXIST: { lookupString: "xxx_val", itemText: "xxx_val", tailText: null, typeText: "Int", attributes: "bold" }
// EXIST: { lookupString: "xxx_bar", itemText: "xxx_bar", tailText: "(p1: Int, p2: String)", typeText: "Unit", attributes: "" }
// NOTHING_ELSE
