// test for KT-5337
package test

annotation class A(val value: String)

@A(<error>null</error>)
fun foo() {}

@A(<error>null</error>)
class B