@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.ExperimentalStdlibApi::class,
    kotlin.native.concurrent.ObsoleteWorkersApi::class,
    kotlin.reflect.ExperimentalAssociatedObjects::class,
)

import kotlin.native.concurrent.ThreadLocal
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.reflect.AssociatedObjectKey
import kotlin.reflect.KClass
import kotlinx.cinterop.staticCFunction

// The runtime calls all of these through C function pointers, which arm64e signs with the IA key and no discriminator.

// CHECK-DAG: call void @CallInitGlobalPossiblyLock(ptr noundef {{[^,]+}}, ptr noundef ptrauth (ptr @"kfun:$init_global$c_function_pointers.kt#internal", i32 0)
val global = listOf(1)

// CHECK-DAG: call void @CallInitThreadLocal(ptr noundef {{[^,]+}}, ptr noundef {{[^,]+}}, ptr noundef ptrauth (ptr @"kfun:$init_thread_local$c_function_pointers.kt#internal", i32 0)
@ThreadLocal
val threadLocal = listOf(2)

// CHECK-DAG: @init_node{{[.0-9]*}} = internal global %struct.InitNode { ptr ptrauth (ptr @{{[^,]+}}, i32 0), ptr null }
@Suppress("DEPRECATION")
@EagerInitialization
val eager = listOf(3)

// CHECK-DAG: @"kclass:Holder" = {{.*}}ptr ptrauth (ptr @Kotlin_processObjectInMark, i32 0)
class Holder(val value: Any)

// CHECK-DAG: @"kassociatedobjects:WithAssociatedObject" = {{.*}}ptr ptrauth (ptr @"kfun:Associated#<get-$instance>#static(){}Associated", i32 0)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Key(val kClass: KClass<*>)

object Associated

@Key(Associated::class)
class WithAssociatedObject

// CHECK-DAG: ptrauth (ptr @{{[^,]+}}_kncfun{{[0-9]+}}, i32 0)
fun callback() = staticCFunction { x: Int -> x + 1 }

// CHECK-DAG: call {{.*}}@"kfun:kotlin.native.concurrent#executeImpl{{.*}}, ptr ptrauth (ptr @"kfun:{{[^"]*}}execute$lambda{{[^"]*}}", i32 0))
fun execute(worker: Worker) = worker.execute(TransferMode.SAFE, { 1 }) { it + 1 }

// CHECK-DAG: @Kotlin_getSourceInfo_Function = global ptr ptrauth (ptr @Kotlin_getSourceInfo_libbacktrace, i32 0)
