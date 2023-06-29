@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cnames.structs.ForwardDeclaredStruct
import objcnames.classes.ForwardDeclaredClass
import objcnames.protocols.ForwardDeclaredProtocolProtocol
import a.*
import kotlinx.cinterop.*

fun testStruct(s: Any?) = consumeStruct(s as CPointer<ForwardDeclaredStruct>)
fun testClass(s: Any?) = consumeClass(s as ForwardDeclaredClass)
fun testProtocol(s: Any?) = consumeProtocol(s as ForwardDeclaredProtocolProtocol)


fun <T : ForwardDeclaredClass> testClass2Impl(s: Any?) = consumeClass(s as T)
fun <T : ForwardDeclaredProtocolProtocol> testProtocol2Impl(s: Any?) = consumeProtocol(s as T)

fun testClass2(s: Any?) = testClass2Impl<ForwardDeclaredClass>(s)
fun testProtocol2(s: Any?) = testProtocol2Impl<ForwardDeclaredProtocolProtocol>(s)