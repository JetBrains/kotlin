// DUMP_IR

import org.jetbrains.kotlin.specialization.Monomorphic

abstract class A1
class B1: A1()
class C1: A1()

interface A2
interface B2: A2
class C2: A2

fun <@Monomorphic T1: A1, @Monomorphic T2: A2> testCartesianProduct() {}

fun box() = "OK"