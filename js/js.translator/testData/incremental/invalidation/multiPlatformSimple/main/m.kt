fun box(stepId: Int, isWasm: Boolean): String {
    val lib1Common = stepId
    val lib1 = lib1Common + 1
    val lib2Common = lib1Common + stepId + 1
    val lib2 = lib1Common + lib2Common + lib1 + 1

    var got = lib1CommonFun()
    if (lib1Common != got) {
        return "Fail lib1CommonFun(): $lib1Common != $got"
    }
    got = lib1Fun()
    if (lib1 != got) {
        return "Fail lib1Fun(): $lib1 != $got"
    }
    got = lib2CommonFun()
    if (lib2Common != got) {
        return "Fail lib2CommonFun(): $lib2Common != $got"
    }
    got = lib2Fun()
    if (lib2 != got) {
        return "Fail lib2Fun(): $lib2 != $got"
    }

    return "OK"
}
