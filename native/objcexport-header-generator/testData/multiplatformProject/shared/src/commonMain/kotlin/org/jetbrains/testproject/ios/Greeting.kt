package org.jetbrains.testproject.ios

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.DatePeriodIso8601Serializer

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}

//fun foo(): String {
//    return "empty"
//}
//
//fun getDateTime(): LocalDateTime {
//    return null!!
//}
//
//fun getDatePeriodIso8601Serializer(): DatePeriodIso8601Serializer {
//    return null!!
//}