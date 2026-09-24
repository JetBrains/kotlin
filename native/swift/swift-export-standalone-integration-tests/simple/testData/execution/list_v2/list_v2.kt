// KIND: STANDALONE
// WITH_PLATFORM_LIBS

// MODULE: ListExportObjCInterop

// FILE: listExportObjCInterop.def
language = Objective-C
modules = ListExportObjC
package = list_export_objc

// FILE: ArrayProvider.h
#import <Foundation/Foundation.h>

@protocol Foo
- (int)value;
@end

@protocol FooArrayProvider
- (NSArray<id<Foo>> *)getFooArray;
- (void)setFooArray:(NSArray<id<Foo>> *)array;
@end

// FILE: module.modulemap
module ListExportObjC {
    header "ArrayProvider.h"
    export *
}

// MODULE: ListExport(ListExportObjCInterop)
// SWIFT_EXPORT_CONFIG: collectionsV2=true
// FILE: main.kt

class Box(val x: Int)

fun listOf(vararg elements: Int): List<Int> = elements.asList()
fun reverseListInt(l: List<Int>) = l.reversed()

fun listOf(vararg elements: Short): List<Short> = elements.asList()
fun reverseListShort(l: List<Short>) = l.reversed()

fun listOf(vararg elements: Char): List<Char> = elements.asList()
fun reverseListChar(l: List<Char>) = l.reversed()

fun listOf(vararg elements: String): List<String> = elements.asList()
fun reverseListString(l: List<String>) = l.reversed()

fun listOf(vararg elements: Box): List<Box> = elements.asList()
fun reverseListBox(l: List<Box>) = l.reversed()

fun listOf(vararg elements: Int?): List<Int?> = elements.asList()
fun reverseListOptInt(l: List<Int?>) = l.reversed()

fun listOf(vararg elements: String?): List<String?> = elements.asList()
fun reverseListOptString(l: List<String?>) = l.reversed()

fun listOf(vararg elements: Box?): List<Box?> = elements.asList()
fun reverseListOptBox(l: List<Box?>) = l.reversed()

fun listOf(vararg elements: List<Int>): List<List<Int>> = elements.asList()
fun reverseListListInt(l: List<List<Int>>) = l.reversed()

fun listOf(vararg elements: List<Int>?): List<List<Int>?> = elements.asList()
fun reverseListOptListInt(l: List<List<Int>?>) = l.reversed()

fun reverseOptListInt(l: List<Int>?) = l?.reversed()

fun reverseListNothing(l: List<Nothing>) = l.reversed()
fun reverseListOptNothing(l: List<Nothing?>) = l.reversed()

fun List<Int>.extReverseListInt() = this.reversed()

val List<Int>.extReverseListIntProp
        get() = this.reversed()

fun mutableListOf(vararg elements: Int): MutableList<Int> = elements.toMutableList()

// FILE: objcInterop.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import platform.darwin.NSObject
import list_export_objc.FooProtocol
import list_export_objc.FooArrayProviderProtocol

//private class KotlinFoo(private val value: Int) : NSObject(), FooProtocol {
//    override fun value(): Int = value
//}
//
//private class KotlinFooArrayProvider : NSObject(), FooArrayProviderProtocol {
//    override fun fooArray(): List<FooProtocol> = listOf(KotlinFoo(1))
//}
//
//fun kotlinFooArrayProvider(): FooArrayProviderProtocol = KotlinFooArrayProvider()

// TODO: Test Kotlin implementation

fun listOf(vararg elements: FooProtocol): List<FooProtocol> = elements.asList()

fun getFooList(provider: FooArrayProviderProtocol): List<FooProtocol> = provider.getFooArray() as List<FooProtocol>

fun setFooList(provider: FooArrayProviderProtocol, list: List<FooProtocol>) {
    provider.setFooArray(list)
}
