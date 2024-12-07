expect fun lib2CommonFun(): Int

fun lib2Fun() = lib2CommonFun() + lib1CommonFun() + lib1Fun() + 1
