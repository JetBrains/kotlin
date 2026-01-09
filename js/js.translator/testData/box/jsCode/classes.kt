// FILE: a.kt

fun initClasses() {
    js("""
        class A {
            constructor() { 
                this.ctorCalledA = 1;
                this._positiveValue = 0;
            }
            
            regularMethod(param) { return "regularMethod" + param; }
            
            "stringLiteralNamed"() { return "stringLiteralNamed"; }
            
            123() { return 123; }
            
            12.3() { return 12.3; }
            
            ["computed" + "Named" + "Method"]() { return "computedNamedMethod"; }
            
            static() { return "static"; }
            
            static static() { return "staticCalled"; }
            
            get positiveValue() { return this._positiveValue; }
            
            set positiveValue(val) { 
                if (val < 0) this._positiveValue = 0;
                else this._positiveValue = val;
            }
            
            get() { return "getCalled"; }

            set() { return "setCalled"; }
            
            *generator() { yield 1; yield 2; }
            
            ;;;
        }
        globalThis.A = A;

        class B extends A {
            constructor(value) {
                super();
                this.ctorCalledB = value;
            }
        }
        globalThis.B = B;
    """)
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    initClasses()

    val a = js("new A();")

    if (a.ctorCalledA != 1) return "fail: a.ctorCalledA == ${a.ctorCalledA}"

    if (a.regularMethod(123) != "regularMethod123") return "fail: a.regularMethod() == ${a.regularMethod()}"

    if (a["stringLiteralNamed"]() != "stringLiteralNamed") return "fail: a[stringLiteralNamed]() == ${a["stringLiteralNamed"]()}"

    if (a["123"]() != 123) return "fail: a[123]() == ${a["123"]()}"

    if (a["12.3"]() != 12.3) return "fail: a[12.3]() == ${a["12.3"]()}"

    if (a["computedNamedMethod"]() != "computedNamedMethod") return "fail: a[computedNamedMethod]() == ${a["computedNamedMethod"]()}"

    if (a.`static`() != "static") return "fail: a.static() == ${a.`static`()}"

    val staticMethodValue = js("A.static()")
    if (staticMethodValue != "staticCalled") return "fail: A.static == ${staticMethodValue}"

    a.positiveValue = -1
    if (a.positiveValue != 0) return "fail: a.positiveValue == ${a.positiveValue}"

    if (a.get() != "getCalled") return "fail: a.get() == ${a.get()}"
    if (a.set() != "setCalled") return "fail: a.set() == ${a.set()}"

    val generator = a.generator()
    val one = generator.next().value
    val two = generator.next().value
    if (one != 1) return "fail: a.generator().first == '$one'"
    if (two != 2) return "fail: a.generator().second == '$two'"

    val b = js("new B(123);")

    if (b.ctorCalledA != 1) return "fail: b.ctorCalledA == ${b.ctorCalledA}"
    if (b.ctorCalledB != 123) return "fail: b.ctorCalledB == ${b.ctorCalledB}"

    return "OK"
}