import kotlinx.serialization.ExperimentalSerializationApi

annotation class LocalAnnotation

@ExperimentalSerializationApi
val experimentVal = ""

@ExperimentalSerializationApi
var experimentVar = ""

@LocalAnnotation
val localVal = ""

@LocalAnnotation
var localVar = ""