package second

import third.Dependency as MyDependency

fun MyDependency?.helloFun() {
}

fun <T: MyDependency> T.helloFunGeneric() {
}
