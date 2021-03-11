package using.libs

import lib2.extendBase

//check that references to lib1 entities obtained through lib2 are valid
fun main() {
    lib1.acceptBase(lib1.Base())
    lib2.acceptBase(lib1.Base())
    lib2.acceptBase(lib2.Derived())
    lib1.acceptBase(lib2.Derived())
    lib1.acceptBase(lib2.returnBase())
    lib1.Base().extendBase()
    lib1.Base().baseFun()
    lib2.Derived().baseFun()
    lib2.Derived().derivedFun()
}