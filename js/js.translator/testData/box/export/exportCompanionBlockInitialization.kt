// RUN_PLAIN_BOX_FUNCTION
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// FILE: lib.kt
var initLog = ""

private fun init(tag: String, value: String): String {
    initLog += tag
    return value
}

@JsExport
class ExportedHost {
    companion {
        val readOnly = init("R", "O")
        var mutable = init("M", "")

        fun append(value: String = "K"): String {
            mutable = value
            return readOnly + mutable
        }
    }
}

@JsExport
fun readExportedHostLog(): String = initLog

@JsExport
class ThrowingExportedCompanionBlock {
    companion {
        val never: String = run {
            throw IllegalStateException("ThrowingExportedCompanionBlock.never")
        }

        val after = "after"
    }
}

// FILE: main.js
function expectThrows(label, expectedName, action) {
    try {
        action();
        return "FAIL: " + label + " did not throw";
    } catch (e) {
        if (e.name !== expectedName) return "FAIL: " + label + " threw " + e.name;
        return null;
    }
}

function box() {
    var ExportedHost = this.lib.ExportedHost;
    var ThrowingExportedCompanionBlock = this.lib.ThrowingExportedCompanionBlock;

    if (this.lib.readExportedHostLog() !== "") return "FAIL: initial log"
    if (ExportedHost.append("K") !== "OK") return "FAIL: append first"
    if (this.lib.readExportedHostLog() !== "RM") return "FAIL: log after append"
    if (ExportedHost.append() !== "OK") return "FAIL: append default"
    if (this.lib.readExportedHostLog() !== "RM") return "FAIL: double init"

    var firstFailure = expectThrows("throwing first access", "ExceptionInInitializerError", function () {
        return ThrowingExportedCompanionBlock.never;
    });
    if (firstFailure !== null) return firstFailure;

    var secondFailure = expectThrows("throwing second access", "NoClassDefFoundError", function () {
        return ThrowingExportedCompanionBlock.after;
    });
    if (secondFailure !== null) return secondFailure;

    return "OK"
}
