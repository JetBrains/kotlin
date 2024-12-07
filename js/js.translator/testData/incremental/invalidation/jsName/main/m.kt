fun box(step: Int): String {
    val x = TestClass()

    if (x.bar() != "Bar") return "Fail bar in Bar"
    if (x.foo() != "FooImpl") return "Fail overridden foo in FooBarImpl"

    when (step) {
        0 -> {
           if (x.default() != "Default") return "Fail default in Foo"
        }
        1 -> {
            if (x.default() != "TestClass") return "Fail default in TestClass"
        }
        2 -> {
            if (x.asDynamic()._foo_() != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsName"
        }
        3 -> {
            if (x.asDynamic()._foo_() != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsName"
            if (x.asDynamic()._default_() != "TestClass") return "Fail default in TestClass with @JsName"
        }
        4 -> {
            if (x.asDynamic()._foo_() != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsName"
            if (x.asDynamic()._default_() != "TestClass") return "Fail default in TestClass with @JsName"
            if (x.asDynamic()._bar_() != "Bar") return "Fail bar in Bar with @JsName"
        }
        5 -> {
            if (x.asDynamic()._foo_() != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsName"
            if (x.asDynamic()._default_() != "Default") return "Fail default in Default with @JsName"
            if (x.asDynamic()._bar_() != "Bar") return "Fail bar in Bar with @JsName"
        }
    }

    return "OK"
}