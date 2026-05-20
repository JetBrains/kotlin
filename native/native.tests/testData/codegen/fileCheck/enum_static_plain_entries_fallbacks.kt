// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=staticPlainEnumEntries=true

enum class PayloadEnum(val payload: Int) {
    Z(10),
    A(20),
}

// CHECK-LABEL: define internal void @"kfun:PayloadEnum.$init_global#internal"
// CHECK: call ptr @AllocInstance
// CHECK: call void @"kfun:PayloadEnum#<init>(kotlin.String;kotlin.Int;kotlin.Int){}"

enum class EntryBodyEnum {
    Z {
        override fun marker(): String = "Z-body"
    },
    A;

    open fun marker(): String = name
}

// CHECK-LABEL: define internal void @"kfun:EntryBodyEnum.$init_global#internal"
// CHECK: call ptr @AllocInstance
// CHECK: call void @"kfun:EntryBodyEnum.EntryBodyEnum$Z.<init>#internal"

var initLog = ""

enum class InitEnum {
    Z,
    A;

    init {
        initLog += name
    }
}

// CHECK-LABEL: define internal void @"kfun:InitEnum.$init_global#internal"
// CHECK: call ptr @AllocInstance
// CHECK: call void @"kfun:InitEnum#<init>(kotlin.String;kotlin.Int){}"

fun box(): String {
    if (PayloadEnum.values().map { it.payload }.joinToString() != "10, 20") return "FAIL payload"
    if (EntryBodyEnum.Z.marker() != "Z-body") return "FAIL body"
    if (EntryBodyEnum.A.marker() != "A") return "FAIL body default"
    if (InitEnum.values().map { it.name }.joinToString() != "Z, A") return "FAIL init values"
    if (initLog != "ZA") return "FAIL init side effects: $initLog"
    return "OK"
}
