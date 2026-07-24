fun foo(): Int = 2

@JsFun("() => require(\"dependency\").id")
external fun loadDependencyViaRequire(): String

@JsFun("""
    () => {
        try {
            require("dependency")
            return "OK"
        } catch (e) {
            return String(e.message)
        }
    }
""")
external fun loadDependencyViaRequireErrorMessage(): String
