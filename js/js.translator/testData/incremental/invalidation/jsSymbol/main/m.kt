fun box(step: Int): String {
    val x = TestClass()

    val toPrimitive = js("Symbol.toPrimitive")
    val match = js("Symbol.match")
    val iterator = js("Symbol.iterator")

    if (x.bar() != "Bar") return "Fail bar in Bar"
    if (x.foo("h") != "FooImpl") return "Fail overridden foo in FooBarImpl"

    when (step) {
        0 -> {
           if (x.default() != "Default") return "Fail default in Foo"
        }
        1 -> {
            if (x.default() != "TestClass") return "Fail default in TestClass"
        }
        2 -> {
            val symFoo = x.asDynamic()[toPrimitive]
            if (symFoo == undefined) return "Fail Symbol.toPrimitive missing on foo at step 2"
            if (x.asDynamic()[toPrimitive]("H") != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsSymbol(toPrimitive)"
        }
        3 -> {
            if (x.asDynamic()[toPrimitive]("H") != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsSymbol(toPrimitive)"
            val symDefault = x.asDynamic()[match]
            if (symDefault == undefined) return "Fail Symbol.match missing on default at step 3"
            if (x.asDynamic()[match]("Z") != "TestClass") return "Fail default in TestClass with @JsSymbol(match)"
        }
        4 -> {
            if (x.asDynamic()[toPrimitive]("H") != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsSymbol(toPrimitive)"
            if (x.asDynamic()[match]("Z") != "TestClass") return "Fail default in TestClass with @JsSymbol(match)"
            val symBar = x.asDynamic()[iterator]
            if (symBar == undefined) return "Fail Symbol.iterator missing on bar at step 4"
            if (x.asDynamic()[iterator]() != "Bar") return "Fail bar in Bar with @JsSymbol(iterator)"
        }
        5 -> {
            if (x.asDynamic()[toPrimitive]("H") != "FooImpl") return "Fail overridden foo in FooBarImpl with @JsSymbol(toPrimitive)"
            if (x.asDynamic()[match]("Z") != "Default") return "Fail default in Default with @JsSymbol(match) after override removal"
            if (x.asDynamic()[iterator]() != "Bar") return "Fail bar in Bar with @JsSymbol(iterator)"
        }
    }

    return "OK"
}
