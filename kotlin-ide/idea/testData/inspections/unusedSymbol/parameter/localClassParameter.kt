//KT-8137 AE at JetSourceNavigationHelper.convertNamedClassOrObject() on function local class with several constructor parameters
//EA-69470
fun main(args: Array<String>) {
    class RealFun(val localClassParameter: String)

    RealFun(localClassParameter = "").localClassParameter
}