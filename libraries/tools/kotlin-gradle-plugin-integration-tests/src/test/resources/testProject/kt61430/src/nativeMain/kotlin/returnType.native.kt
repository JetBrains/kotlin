import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

fun <K : Any?, V : Any?> returnTypeShared(field: KMutableProperty1<K, V>): KType {
    return field.returnType
}
