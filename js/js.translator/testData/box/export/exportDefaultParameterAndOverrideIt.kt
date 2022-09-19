// EXPECTED_REACHABLE_NODES: 1252
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_DCE_DRIVEN
// IGNORE_BACKEND: JS

// MODULE: export_default_method
// FILE: lib.kt

@JsExport
abstract class Adapter {
    abstract fun method(o: String = "O", k: String = "K")
}

@JsExport
class TwoArgumentsWrapper(val adapter: Adapter) {
    fun method(a: String, b: String) {
        adapter.method(a, b)
    }
}

@JsExport
class OneArgumentWrapper(val adapter: Adapter) {
    fun method(param: String) {
        adapter.method(param)
    }
}

@JsExport
class NoArgumentsWrapper(val adapter: Adapter) {
    fun method() {
        adapter.method()
    }
}


// FILE: test.js

function box() {
    const Adapter = this.export_default_method.Adapter
    const Wrapper2 = this.export_default_method.TwoArgumentsWrapper
    const Wrapper1 = this.export_default_method.OneArgumentWrapper
    const Wrapper0 = this.export_default_method.NoArgumentsWrapper

    class WebAdapter extends Adapter {
        constructor(name) {
            super()
            this.resetError(name)
        }

        method(o = "O", k = "K") {
            this.result = o + k
        }

        resetError(name)  {
            this.result = `Error: ${name}'s overridden method was not called`
        }
    }

    const webAdapter = new WebAdapter("webAdapter2")

    const wrapper2 = new Wrapper2(webAdapter)
    wrapper2.method("O", "K")

    if (webAdapter.result !== "OK") return webAdapter.result

    webAdapter.resetError("webAdapter1")

    const wrapper1 = new Wrapper1(webAdapter)
    wrapper1.method("O")

    if (webAdapter.result !== "OK") return webAdapter.result

    webAdapter.resetError("webAdapter0")

    const wrapper0 = new Wrapper0(webAdapter)
    wrapper0.method()

    if (webAdapter.result !== "OK") return webAdapter.result

    return "OK"
}