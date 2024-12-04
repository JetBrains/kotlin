import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

public fun <K : Any?, V : Any?> returnTypeLinux(field: KMutableProperty1<K, V>): KType {
    return field.returnType
}