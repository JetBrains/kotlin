/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cinterop.*
import objcSmoke.*
import kotlin.native.concurrent.*
import kotlin.native.ref.*
import kotlin.test.*

fun main(args: Array<String>) {
    // Test relies on full deinitialization at shutdown.
    kotlin.native.internal.Debugging.forceCheckedShutdown = true
    autoreleasepool {
        run()
    }
}

fun run() {
    // TODO: migrate remaining tests to interop/objc/tests/
    testTypeOps()
    testCustomRetain()
    testExportObjCClass()
    testLocalizedStrings()

    assertEquals(2, ForwardDeclaredEnum.TWO.value)

    println(
            getSupplier(
                    invoke1(42) { it * 2 }
            )!!()
    )

    val foo = Foo()

    val classGetter = getClassGetter(foo)
    invoke2 { println(classGetter()) }

    foo.hello()
    foo.name = "everybody"
    foo.helloWithPrinter(object : NSObject(), PrinterProtocol {
        override fun print(string: CPointer<ByteVar>?) {
            println("Kotlin says: " + string?.toKString())
        }
    })

    Bar().hello()

    val pair = MutablePairImpl(42, 17)
    replacePairElements(pair, 1, 2)
    pair.swap()
    println("${pair.first}, ${pair.second}")

    val defaultPair = MutablePairImpl()
    assertEquals(defaultPair.first(), 123)
    assertEquals(defaultPair.second(), 321)

    // equals and hashCode (virtually):
    val map = mapOf(foo to pair, pair to foo)

    // equals (directly):
    if (!foo.equals(pair)) {
        // toString (directly):
        println(map[pair].toString() + map[foo].toString() == foo.description() + pair.description())
    }

    // hashCode (directly):
    // hash() returns value of NSUInteger type.
    val hash = if (Platform.osFamily == OsFamily.WATCHOS && Platform.cpuArchitecture.bitness == 32) {
        // `typedef unsigned int NSInteger` on watchOS.
        foo.hash().toInt()
    } else {
        // `typedef unsigned long NSUInteger` on iOS, macOS, tvOS.
        foo.hash().let { it.toInt() xor (it shr 32).toInt() }
    }
    if (foo.hashCode() == hash) {
        // toString (virtually):
        if (Platform.memoryModel == MemoryModel.STRICT)
            println(map.keys.map { it.toString() }.min() == foo.description())
        else
            // TODO: hack until proper cycle collection in maps.
            println(true)
    }
    println(globalString)
    autoreleasepool {
        globalString = "Another global string"
    }
    println(globalString)

    println(globalObject)
    globalObject = object : NSObject() {
        override fun description() = "global object"
    }
    println(globalObject)
    globalObject = null // Prevent Kotlin object above from leaking.

    println(formatStringLength("%d %d", 42, 17))

    println(STRING_MACRO)
    println(CFSTRING_MACRO)

    // Ensure that overriding method bridge has retain-autorelease sequence:
    createObjectWithFactory(object : NSObject(), ObjectFactoryProtocol {
        override fun create() = autoreleasepool { NSObject() }
    })

}

fun MutablePairProtocol.swap() {
    update(0, add = second)
    update(1, sub = first)
    update(0, add = second)
    update(1, sub = second*2)
}

class Bar : Foo() {
    override fun helloWithPrinter(printer: PrinterProtocol?) = memScoped {
        printer!!.print("Hello from Kotlin".cstr.getPointer(memScope))
    }
}

@Suppress("CONFLICTING_OVERLOADS")
class MutablePairImpl(first: Int, second: Int) : NSObject(), MutablePairProtocol {
    private var elements = intArrayOf(first, second)

    override fun first() = elements.first()
    override fun second() = elements.last()

    override fun update(index: Int, add: Int) {
        elements[index] += add
    }

    override fun update(index: Int, sub: Int) {
        elements[index] -= sub
    }

    constructor() : this(123, 321)
}

interface Zzz

fun testTypeOps() {
    assertTrue(99.asAny() is NSNumber)
    assertTrue(null.asAny() is NSNumber?)
    assertFalse(null.asAny() is NSNumber)
    assertFalse("".asAny() is NSNumber)
    assertTrue("bar".asAny() is NSString)

    assertTrue(Foo.asAny() is FooMeta)
    assertFalse(Foo.asAny() is Zzz)
    assertTrue(Foo.asAny() is NSObjectMeta)
    assertTrue(Foo.asAny() is NSObject)
    assertFalse(Foo.asAny() is Foo)
    assertTrue(NSString.asAny() is NSCopyingProtocolMeta)
    assertFalse(NSString.asAny() is NSCopyingProtocol)
    assertTrue(NSValue.asAny() is NSObjectProtocolMeta)
    assertFalse(NSValue.asAny() is NSObjectProtocol) // Must be true, but not implemented properly yet.

    assertFalse(Any() is ObjCClass)
    assertFalse(Any() is ObjCClassOf<*>)
    assertFalse(NSObject().asAny() is ObjCClass)
    assertFalse(NSObject().asAny() is ObjCClassOf<*>)
    assertTrue(NSObject.asAny() is ObjCClass)
    assertTrue(NSObject.asAny() is ObjCClassOf<*>)

    assertFalse(Any() is ObjCProtocol)
    assertTrue(getPrinterProtocolRaw() is ObjCProtocol)
    val printerProtocol = getPrinterProtocol()!!
    assertTrue(printerProtocol.asAny() is ObjCProtocol)

    assertEquals(3u, ("foo" as NSString).length())
    assertEquals(4u, ((1..4).joinToString("") as NSString).length())
    assertEquals(2u, (listOf(0, 1) as NSArray).count())
    assertEquals(42L, (42 as NSNumber).longLongValue())

    assertFails { "bar" as NSNumber }
    assertFails { 42 as NSArray }
    assertFails { listOf(1) as NSString }
    assertFails { NSObject() as Bar }
    assertFails { NSObject() as NSValue }

    MutablePairImpl(1, 2).asAny() as MutablePairProtocol
    assertFails { MutablePairImpl(1, 2).asAny() as Foo }
}

private lateinit var retainedMustNotBeDeallocated: MustNotBeDeallocated

fun testCustomRetain() {
    fun test() {
        useCustomRetainMethods(object : Foo(), CustomRetainMethodsProtocol {
            override fun returnRetained(obj: Any?) = obj
            override fun consume(obj: Any?) {}
            override fun consumeSelf() {}
            override fun returnRetainedBlock(block: (() -> Unit)?) = block
        })

        CustomRetainMethodsImpl().let {
            it.returnRetained(Any())
            retainedMustNotBeDeallocated = MustNotBeDeallocated() // Retain to detect possible over-release.
            it.consume(retainedMustNotBeDeallocated)
            it.consumeSelf()
            it.returnRetainedBlock({})!!()
        }
    }

    autoreleasepool {
        test()
        kotlin.native.internal.GC.collect()
    }

    assertFalse(unexpectedDeallocation)
}

private const val TestExportObjCClass1Name = "TestExportObjCClass"
@ExportObjCClass(TestExportObjCClass1Name) class TestExportObjCClass1 : NSObject()

@ExportObjCClass class TestExportObjCClass2 : NSObject()

const val TestExportObjCClass34Name = "TestExportObjCClass34"
@ExportObjCClass(TestExportObjCClass34Name) class TestExportObjCClass3 : NSObject()
@ExportObjCClass(TestExportObjCClass34Name) class TestExportObjCClass4 : NSObject()

fun testExportObjCClass() {
    assertEquals(TestExportObjCClass1Name, TestExportObjCClass1().objCClassName)
    assertEquals("TestExportObjCClass2", TestExportObjCClass2().objCClassName)

    assertTrue((TestExportObjCClass3().objCClassName == TestExportObjCClass34Name)
            xor (TestExportObjCClass4().objCClassName == TestExportObjCClass34Name))
}

fun testLocalizedStrings() {
    val key = "screen_main_plural_string"
    val localizedString = NSBundle.mainBundle.localizedStringForKey(key, value = "", table = "Localizable")
    val string = NSString.localizedStringWithFormat(localizedString, 5)
    assertEquals("Plural: 5 apples", string)
}

private val Any.objCClassName: String
    get() = object_getClassName(this)!!.toKString()

fun Any?.asAny(): Any? = this
