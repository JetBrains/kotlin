// FILE: a.kt

fun initClasses() {
    js("""
        class A {
            constructor() { 
                this.ctorCalledA = 1;
                this._positiveValue = 0;
            }
            
            regularMethod(param) { return "regularMethod" + param; }
            
            regularMethodDefault(param = "Fallback") { return "regularMethodDefault" + param; }
            
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
            
            set positiveValueDefault(val = 123) {
                this.positiveValue = val;
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
        
        class C extends B {
            constructor(value = "fallback") {
                super(123);
                this.ctorCalledC = value;
            }
        }
        globalThis.C = C;
        
        globalThis.D = class { 
            constructor() {
                this.ctorCalledD = 1;
            }
        };
        
        globalThis.E = class E {
            constructor() {
                this.ctorCalledE = 1;
            }
        };
        
        globalThis.F = class F extends globalThis.E {
            constructor() {
                super();
                this.ctorCalledF = 1;
            }
        };
        
        globalThis.G = class extends globalThis.F {
            constructor() {
                super();
                this.ctorCalledG = 1;
            }
        };
    """)
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    val jsUndefined = js("undefined")

    initClasses()

    val a = js("new A();")

    if (a.ctorCalledA != 1) return "fail: a.ctorCalledA == ${a.ctorCalledA}"

    if (a.regularMethod(123) != "regularMethod123") return "fail: a.regularMethod() == ${a.regularMethod()}"

    if (a.regularMethodDefault() != "regularMethodDefaultFallback") return "fail: a.regularMethodDefault() == ${a.regularMethodDefault()}"

    if (a["stringLiteralNamed"]() != "stringLiteralNamed") return "fail: a[stringLiteralNamed]() == ${a["stringLiteralNamed"]()}"

    if (a["123"]() != 123) return "fail: a[123]() == ${a["123"]()}"

    if (a["12.3"]() != 12.3) return "fail: a[12.3]() == ${a["12.3"]()}"

    if (a["computedNamedMethod"]() != "computedNamedMethod") return "fail: a[computedNamedMethod]() == ${a["computedNamedMethod"]()}"

    if (a.`static`() != "static") return "fail: a.static() == ${a.`static`()}"

    val staticMethodValue = js("A.static()")
    if (staticMethodValue != "staticCalled") return "fail: A.static == ${staticMethodValue}"

    a.positiveValue = -1
    if (a.positiveValue != 0) return "fail: a.positiveValue after a.positiveValue set == ${a.positiveValue}"

    a.positiveValueDefault = jsUndefined
    if (a.positiveValue != 123) return "fail: a.positiveValue after a.positiveValueDefault set == ${a.positiveValue}"

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

    val c = js("new C();")

    if (c.ctorCalledA != 1) return "fail: c.ctorCalledA == ${c.ctorCalledA}"
    if (c.ctorCalledB != 123) return "fail: c.ctorCalledB == ${c.ctorCalledB}"
    if (c.ctorCalledC != "fallback") return "fail: c.ctorCalledC == ${c.ctorCalledC}"

    val d = js("new D();")

    if (d.ctorCalledD != 1) return "fail: d.ctorCalledD == ${d.ctorCalledD}"

    val e = js("new E();")

    if (e.ctorCalledE != 1) return "fail: e.ctorCalledE == ${e.ctorCalledE}"

    val f = js("new F();")

    if (f.ctorCalledE != 1) return "fail: f.ctorCalledE == ${f.ctorCalledE}"
    if (f.ctorCalledF != 1) return "fail: f.ctorCalledF == ${f.ctorCalledF}"

    val g = js("new G();")

    if (g.ctorCalledF != 1) return "fail: g.ctorCalledF == ${g.ctorCalledF}"
    if (g.ctorCalledG != 1) return "fail: g.ctorCalledG == ${g.ctorCalledG}"

    val h = js("""new class extends G {
        constructor() {
            super();
            this.ctorCalledH = 1;
        }
    }();""")

    if (h.ctorCalledG != 1) return "fail: h.ctorCalledG == ${h.ctorCalledG}"
    if (h.ctorCalledH != 1) return "fail: h.ctorCalledH == ${h.ctorCalledH}"

    return "OK"
}