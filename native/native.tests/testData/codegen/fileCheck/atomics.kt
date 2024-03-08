// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlin.concurrent.*

// CHECK-AAPCS-LABEL: define i8 @"kfun:#<get-byteGlobal>(){}kotlin.Byte"()
// CHECK-DEFAULTABI-LABEL: define signext i8 @"kfun:#<get-byteGlobal>(){}kotlin.Byte"()
// CHECK-WINDOWSX64-LABEL: define i8 @"kfun:#<get-byteGlobal>(){}kotlin.Byte"()
// CHECK: load atomic i8, ptr @"kvar:byteGlobal#internal" seq_cst
// CHECK-LABEL: define void @"kfun:#<set-byteGlobal>(kotlin.Byte){}"(i8
// CHECK: store atomic i8 %{{[0-9]+}}, ptr @"kvar:byteGlobal#internal" seq_cst
@Volatile var byteGlobal: Byte = 42

// CHECK-AAPCS-LABEL: define i16 @"kfun:#<get-shortGlobal>(){}kotlin.Short"()
// CHECK-DEFAULTABI-LABEL: define signext i16 @"kfun:#<get-shortGlobal>(){}kotlin.Short"()
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#<get-shortGlobal>(){}kotlin.Short"()
// CHECK: load atomic i16, ptr @"kvar:shortGlobal#internal" seq_cst
// CHECK-LABEL: define void @"kfun:#<set-shortGlobal>(kotlin.Short){}"(i16
// CHECK: store atomic i16 %{{[0-9]+}}, ptr @"kvar:shortGlobal#internal" seq_cst
@Volatile var shortGlobal: Short = 42

// CHECK-LABEL: define i32 @"kfun:#<get-intGlobal>(){}kotlin.Int"()
// CHECK: load atomic i32, ptr @"kvar:intGlobal#internal" seq_cst
// CHECK-LABEL: define void @"kfun:#<set-intGlobal>(kotlin.Int){}"(i32
// CHECK: store atomic i32 %{{[0-9]+}}, ptr @"kvar:intGlobal#internal" seq_cst
@Volatile var intGlobal: Int = 42

// CHECK-LABEL: define i64 @"kfun:#<get-longGlobal>(){}kotlin.Long"()
// CHECK: load atomic i64, ptr @"kvar:longGlobal#internal" seq_cst
// CHECK-LABEL: define void @"kfun:#<set-longGlobal>(kotlin.Long){}"(i64
// CHECK: store atomic i64 %{{[0-9]+}}, ptr @"kvar:longGlobal#internal" seq_cst
@Volatile var longGlobal: Long = 42

// CHECK-AAPCS-LABEL: define i1 @"kfun:#<get-booleanGlobal>(){}kotlin.Boolean"()
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#<get-booleanGlobal>(){}kotlin.Boolean"()
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#<get-booleanGlobal>(){}kotlin.Boolean"()
// CHECK: load atomic i8, ptr @"kvar:booleanGlobal#internal" seq_cst
// CHECK-LABEL: define void @"kfun:#<set-booleanGlobal>(kotlin.Boolean){}"(i1
// CHECK: store atomic i8 %{{[0-9]+}}, ptr @"kvar:booleanGlobal#internal" seq_cst
@Volatile var booleanGlobal: Boolean = true

// Byte
fun byteGlobal_getField() = byteGlobal
fun byteGlobal_setField() { byteGlobal = 0 }

// CHECK-AAPCS-LABEL: define i8 @"kfun:#byteGlobal_getAndSetField(){}kotlin.Byte"()
// CHECK-DEFAULTABI-LABEL: define signext i8 @"kfun:#byteGlobal_getAndSetField(){}kotlin.Byte"()
// CHECK-WINDOWSX64-LABEL: define i8 @"kfun:#byteGlobal_getAndSetField(){}kotlin.Byte"()
// CHECK: atomicrmw xchg ptr @"kvar:byteGlobal#internal", i8 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun byteGlobal_getAndSetField() = ::byteGlobal.getAndSetField(0.toByte())

// CHECK-AAPCS-LABEL: define i8 @"kfun:#byteGlobal_getAndAddField(){}kotlin.Byte"()
// CHECK-DEFAULTABI-LABEL: define signext i8 @"kfun:#byteGlobal_getAndAddField(){}kotlin.Byte"()
// CHECK-WINDOWSX64-LABEL: define i8 @"kfun:#byteGlobal_getAndAddField(){}kotlin.Byte"()
// CHECK: atomicrmw add ptr @"kvar:byteGlobal#internal", i8 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun byteGlobal_getAndAddField() = ::byteGlobal.getAndAddField(0.toByte())

// CHECK-AAPCS-LABEL: define i1 @"kfun:#byteGlobal_compareAndSetField(){}kotlin.Boolean"()
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#byteGlobal_compareAndSetField(){}kotlin.Boolean"()
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#byteGlobal_compareAndSetField(){}kotlin.Boolean"()
// CHECK: cmpxchg ptr @"kvar:byteGlobal#internal", i8 0, i8 1 seq_cst seq_cst
// CHECK: extractvalue { i8, i1 } %{{[0-9]+}}, 1
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun byteGlobal_compareAndSetField() = ::byteGlobal.compareAndSetField(0.toByte(), 1.toByte())

// CHECK-AAPCS-LABEL: define i8 @"kfun:#byteGlobal_compareAndExchangeField(){}kotlin.Byte"()
// CHECK-DEFAULTABI-LABEL: define signext i8 @"kfun:#byteGlobal_compareAndExchangeField(){}kotlin.Byte"()
// CHECK-WINDOWSX64-LABEL: define i8 @"kfun:#byteGlobal_compareAndExchangeField(){}kotlin.Byte"()
// CHECK: cmpxchg ptr @"kvar:byteGlobal#internal", i8 0, i8 1 seq_cst seq_cst
// CHECK: extractvalue { i8, i1 } %{{[0-9]+}}, 0
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun byteGlobal_compareAndExchangeField() = ::byteGlobal.compareAndExchangeField(0.toByte(), 1.toByte())

// Short
fun shortGlobal_getField() = shortGlobal
fun shortGlobal_setField() { shortGlobal = 0 }

// CHECK-AAPCS-LABEL: define i16 @"kfun:#shortGlobal_getAndSetField(){}
// CHECK-DEFAULTABI-LABEL: define signext i16 @"kfun:#shortGlobal_getAndSetField(){}
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#shortGlobal_getAndSetField(){}
// CHECK: atomicrmw xchg ptr @"kvar:shortGlobal#internal", i16 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun shortGlobal_getAndSetField() = ::shortGlobal.getAndSetField(0.toShort())

// CHECK-AAPCS-LABEL: define i16 @"kfun:#shortGlobal_getAndAddField(){}
// CHECK-DEFAULTABI-LABEL: define signext i16 @"kfun:#shortGlobal_getAndAddField(){}
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#shortGlobal_getAndAddField(){}
// CHECK: atomicrmw add ptr @"kvar:shortGlobal#internal", i16 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun shortGlobal_getAndAddField() = ::shortGlobal.getAndAddField(0.toShort())

// CHECK-AAPCS-LABEL: define i1 @"kfun:#shortGlobal_compareAndSetField(){}
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#shortGlobal_compareAndSetField(){}
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#shortGlobal_compareAndSetField(){}
// CHECK: cmpxchg ptr @"kvar:shortGlobal#internal", i16 0, i16 1 seq_cst seq_cst
// CHECK: extractvalue { i16, i1 } %{{[0-9]+}}, 1
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun shortGlobal_compareAndSetField() = ::shortGlobal.compareAndSetField(0.toShort(), 1.toShort())

// CHECK-AAPCS-LABEL: define i16 @"kfun:#shortGlobal_compareAndExchangeField(){}
// CHECK-DEFAULTABI-LABEL: define signext i16 @"kfun:#shortGlobal_compareAndExchangeField(){}
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#shortGlobal_compareAndExchangeField(){}
// CHECK: cmpxchg ptr @"kvar:shortGlobal#internal", i16 0, i16 1 seq_cst seq_cst
// CHECK: extractvalue { i16, i1 } %{{[0-9]+}}, 0
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun shortGlobal_compareAndExchangeField() = ::shortGlobal.compareAndExchangeField(0.toShort(), 1.toShort())

// Int
fun intGlobal_getField() = intGlobal
fun intGlobal_setField() { intGlobal = 0 }

// CHECK-LABEL: define i32 @"kfun:#intGlobal_getAndSetField(){}
// CHECK: atomicrmw xchg ptr @"kvar:intGlobal#internal", i32 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intGlobal_getAndSetField() = ::intGlobal.getAndSetField(0)

// CHECK-LABEL: define i32 @"kfun:#intGlobal_getAndAddField(){}
// CHECK: atomicrmw add ptr @"kvar:intGlobal#internal", i32 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intGlobal_getAndAddField() = ::intGlobal.getAndAddField(0)

// CHECK-AAPCS-LABEL: define i1 @"kfun:#intGlobal_compareAndSetField(){}
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#intGlobal_compareAndSetField(){}
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#intGlobal_compareAndSetField(){}
// CHECK: cmpxchg ptr @"kvar:intGlobal#internal", i32 0, i32 1 seq_cst seq_cst
// CHECK: extractvalue { i32, i1 } %{{[0-9]+}}, 1
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intGlobal_compareAndSetField() = ::intGlobal.compareAndSetField(0, 1)

// CHECK-LABEL: define i32 @"kfun:#intGlobal_compareAndExchangeField(){}
// CHECK: cmpxchg ptr @"kvar:intGlobal#internal", i32 0, i32 1 seq_cst seq_cst
// CHECK: extractvalue { i32, i1 } %{{[0-9]+}}, 0
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intGlobal_compareAndExchangeField() = ::intGlobal.compareAndExchangeField(0, 1)

// Long
fun longGlobal_getField() = longGlobal
fun longGlobal_setField() { longGlobal = 0 }

// CHECK-LABEL: define i64 @"kfun:#longGlobal_getAndSetField(){}
// CHECK: atomicrmw xchg ptr @"kvar:longGlobal#internal", i64 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longGlobal_getAndSetField() = ::longGlobal.getAndSetField(0L)

// CHECK-LABEL: define i64 @"kfun:#longGlobal_getAndAddField(){}
// CHECK: atomicrmw add ptr @"kvar:longGlobal#internal", i64 0 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longGlobal_getAndAddField() = ::longGlobal.getAndAddField(0L)

// CHECK-AAPCS-LABEL: define i1 @"kfun:#longGlobal_compareAndSetField(){}
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#longGlobal_compareAndSetField(){}
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#longGlobal_compareAndSetField(){}
// CHECK: cmpxchg ptr @"kvar:longGlobal#internal", i64 0, i64 1 seq_cst seq_cst
// CHECK: extractvalue { i64, i1 } %{{[0-9]+}}, 1
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longGlobal_compareAndSetField() = ::longGlobal.compareAndSetField(0L, 1L)

// CHECK-LABEL: define i64 @"kfun:#longGlobal_compareAndExchangeField(){}
// CHECK: cmpxchg ptr @"kvar:longGlobal#internal", i64 0, i64 1 seq_cst seq_cst
// CHECK: extractvalue { i64, i1 } %{{[0-9]+}}, 0
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longGlobal_compareAndExchangeField() = ::longGlobal.compareAndExchangeField(0L, 1L)

// Boolean
fun booleanGlobal_getField() = booleanGlobal
fun booleanGlobal_setField() { booleanGlobal = false }

// CHECK-AAPCS-LABEL: define i1 @"kfun:#booleanGlobal_getAndSetField(){}kotlin.Boolean"
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#booleanGlobal_getAndSetField(){}kotlin.Boolean"
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#booleanGlobal_getAndSetField(){}kotlin.Boolean"
// CHECK: atomicrmw xchg ptr @"kvar:booleanGlobal#internal", i8 %{{[0-9]+}} seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun booleanGlobal_getAndSetField() = ::booleanGlobal.getAndSetField(false)

// CHECK-AAPCS-LABEL: define i1 @"kfun:#booleanGlobal_compareAndSetField(){}
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#booleanGlobal_compareAndSetField(){}
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#booleanGlobal_compareAndSetField(){}
// CHECK: cmpxchg ptr @"kvar:booleanGlobal#internal", i8 %{{[0-9]+}}, i8 %{{[0-9]+}} seq_cst seq_cst
// CHECK: extractvalue { i8, i1 } %{{[0-9]+}}, 1
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun booleanGlobal_compareAndSetField() = ::booleanGlobal.compareAndSetField(false, true)

// CHECK-AAPCS-LABEL: define i1 @"kfun:#booleanGlobal_compareAndExchangeField(){}
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#booleanGlobal_compareAndExchangeField(){}
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#booleanGlobal_compareAndExchangeField(){}
// CHECK: cmpxchg ptr @"kvar:booleanGlobal#internal", i8 %{{[0-9]+}}, i8 %{{[0-9]+}} seq_cst seq_cst
// CHECK: extractvalue { i8, i1 } %{{[0-9]+}}, 0
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun booleanGlobal_compareAndExchangeField() = ::booleanGlobal.compareAndExchangeField(false, true)

// IntArray
val intArr = IntArray(2)

// CHECK-LABEL: define i32 @"kfun:#intArr_atomicGet(){}kotlin.Int"()
// CHECK: call ptr @Kotlin_intArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: load atomic i32, ptr %{{[0-9]+}} seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intArr_atomicGet() = intArr.atomicGet(0)

// CHECK-LABEL: define void @"kfun:#intArr_atomicSet(){}"()
// CHECK: call ptr @Kotlin_intArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: store atomic i32 1, ptr %{{[0-9]+}} seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intArr_atomicSet() = intArr.atomicSet(0, 1)

// CHECK-LABEL: define i32 @"kfun:#intArr_getAndSet(){}kotlin.Int"()
// CHECK: call ptr @Kotlin_intArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: atomicrmw xchg ptr %{{[0-9]+}}, i32 1 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intArr_getAndSet() = intArr.getAndSet(0, 1)

// CHECK-LABEL: define i32 @"kfun:#intArr_getAndAdd(){}kotlin.Int"()
// CHECK: call ptr @Kotlin_intArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: atomicrmw add ptr %{{[0-9]+}}, i32 1 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intArr_getAndAdd() = intArr.getAndAdd(0, 1)

// CHECK-LABEL: define i32 @"kfun:#intArr_compareAndExchange(){}kotlin.Int"()
// CHECK: call ptr @Kotlin_intArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: cmpxchg ptr %{{[0-9]+}}, i32 1, i32 2 seq_cst seq_cst
// CHECK: extractvalue { i32, i1 } %{{[0-9]+}}, 0
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intArr_compareAndExchange() = intArr.compareAndExchange(0, 1, 2)

// CHECK-AAPCS-LABEL: define i1 @"kfun:#intArr_compareAndSet(){}kotlin.Boolean"()
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#intArr_compareAndSet(){}kotlin.Boolean"()
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#intArr_compareAndSet(){}kotlin.Boolean"()
// CHECK: call ptr @Kotlin_intArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: cmpxchg ptr %{{[0-9]+}}, i32 1, i32 2 seq_cst seq_cst
// CHECK: extractvalue { i32, i1 } %{{[0-9]+}}, 1
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun intArr_compareAndSet() = intArr.compareAndSet(0, 1, 2)

// LongArray
val longArr = LongArray(2)

// CHECK-LABEL: define i64 @"kfun:#longArr_atomicGet(){}kotlin.Long"()
// CHECK: call ptr @Kotlin_longArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: load atomic i64, ptr %{{[0-9]+}} seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longArr_atomicGet() = longArr.atomicGet(0)

// CHECK-LABEL: define void @"kfun:#longArr_atomicSet(){}"()
// CHECK: call ptr @Kotlin_longArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: store atomic i64 1, ptr %{{[0-9]+}} seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longArr_atomicSet() = longArr.atomicSet(0, 1L)

// CHECK-LABEL: define i64 @"kfun:#longArr_getAndSet(){}kotlin.Long"()
// CHECK: call ptr @Kotlin_longArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: atomicrmw xchg ptr %{{[0-9]+}}, i64 1 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longArr_getAndSet() = longArr.getAndSet(0, 1L)

// CHECK-LABEL: define i64 @"kfun:#longArr_getAndAdd(){}kotlin.Long"()
// CHECK: call ptr @Kotlin_longArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: atomicrmw add ptr %{{[0-9]+}}, i64 1 seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longArr_getAndAdd() = longArr.getAndAdd(0, 1L)

// CHECK-LABEL: define i64 @"kfun:#longArr_compareAndExchange(){}kotlin.Long"()
// CHECK: call ptr @Kotlin_longArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: cmpxchg ptr %{{[0-9]+}}, i64 1, i64 2 seq_cst seq_cst
// CHECK: extractvalue { i64, i1 } %{{[0-9]+}}, 0
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longArr_compareAndExchange() = longArr.compareAndExchange(0, 1L, 2L)

// CHECK-AAPCS-LABEL: define i1 @"kfun:#longArr_compareAndSet(){}kotlin.Boolean"()
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#longArr_compareAndSet(){}kotlin.Boolean"()
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#longArr_compareAndSet(){}kotlin.Boolean"()
// CHECK: call ptr @Kotlin_longArrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: cmpxchg ptr %{{[0-9]+}}, i64 1, i64 2 seq_cst seq_cst
// CHECK: extractvalue { i64, i1 } %{{[0-9]+}}, 1
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun longArr_compareAndSet() = longArr.compareAndSet(0, 1L, 2L)

// Array<T>
val refArr = arrayOfNulls<String?>(2)

// CHECK-LABEL: define ptr @"kfun:#refArr_atomicGet(){}kotlin.String?"
// CHECK: call ptr @Kotlin_arrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: load atomic ptr, ptr %{{[0-9]+}} seq_cst
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun refArr_atomicGet() = refArr.atomicGet(0)

// CHECK-LABEL: define void @"kfun:#refArr_atomicSet(){}"()
// CHECK: call ptr @Kotlin_arrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: call void @UpdateVolatileHeapRef(ptr noundef %{{[0-9]+}}, ptr noundef null)
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun refArr_atomicSet() = refArr.atomicSet(0, null)

// CHECK-LABEL: define ptr @"kfun:#refArr_getAndSet(){}kotlin.String?"
// CHECK: call ptr @Kotlin_arrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: call ptr @GetAndSetVolatileHeapRef(ptr noundef %{{[0-9]+}}, ptr noundef null, ptr noundef %{{[0-9]+}})
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun refArr_getAndSet() = refArr.getAndSet(0, null)

// CHECK-LABEL: define ptr @"kfun:#refArr_compareAndExchange(){}kotlin.String?"
// CHECK: call ptr @Kotlin_arrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK: call ptr @CompareAndSwapVolatileHeapRef(ptr noundef %{{[0-9]+}}, ptr noundef null, ptr noundef null, ptr noundef %{{[0-9]+}})
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun refArr_compareAndExchange() = refArr.compareAndExchange(0, null, null)

// CHECK-AAPCS-LABEL: define i1 @"kfun:#refArr_compareAndSet(){}kotlin.Boolean"()
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#refArr_compareAndSet(){}kotlin.Boolean"()
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#refArr_compareAndSet(){}kotlin.Boolean"()
// CHECK: call ptr @Kotlin_arrayGetElementAddress(ptr noundef %{{[0-9]+}}, i32 noundef 0)
// CHECK-AAPCS: call i1 @CompareAndSetVolatileHeapRef(ptr noundef %{{[0-9]+}}, ptr noundef null, ptr noundef null)
// CHECK-DEFAULTABI: call zeroext i1 @CompareAndSetVolatileHeapRef(ptr noundef %{{[0-9]+}}, ptr noundef null, ptr noundef null)
// CHECK-WINDOWSX64: call zeroext i1 @CompareAndSetVolatileHeapRef(ptr noundef %{{[0-9]+}}, ptr noundef null, ptr noundef null)
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun refArr_compareAndSet() = refArr.compareAndSet(0, null, null)

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    byteGlobal_getField()
    byteGlobal_setField()
    byteGlobal_getAndSetField()
    byteGlobal_getAndAddField()
    byteGlobal_compareAndSetField()
    byteGlobal_compareAndExchangeField()

    shortGlobal_getField()
    shortGlobal_setField()
    shortGlobal_getAndSetField()
    shortGlobal_getAndAddField()
    shortGlobal_compareAndSetField()
    shortGlobal_compareAndExchangeField()

    intGlobal_getField()
    intGlobal_setField()
    intGlobal_getAndSetField()
    intGlobal_getAndAddField()
    intGlobal_compareAndSetField()
    intGlobal_compareAndExchangeField()

    longGlobal_getField()
    longGlobal_setField()
    longGlobal_getAndSetField()
    longGlobal_getAndAddField()
    longGlobal_compareAndSetField()
    longGlobal_compareAndExchangeField()

    booleanGlobal_getField()
    booleanGlobal_setField()
    booleanGlobal_getAndSetField()
    booleanGlobal_compareAndSetField()
    booleanGlobal_compareAndExchangeField()

    intArr_atomicGet()
    intArr_atomicSet()
    intArr_getAndSet()
    intArr_getAndAdd()
    intArr_compareAndSet()
    intArr_compareAndExchange()

    longArr_atomicGet()
    longArr_atomicSet()
    longArr_getAndSet()
    longArr_getAndAdd()
    longArr_compareAndSet()
    longArr_compareAndExchange()

    refArr_atomicGet()
    refArr_atomicSet()
    refArr_getAndSet()
    refArr_compareAndSet()
    refArr_compareAndExchange()
    return "OK"
}
