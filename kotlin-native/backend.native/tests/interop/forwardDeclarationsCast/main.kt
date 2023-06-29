@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import a.*
import b.*


fun main() {
	if (testStruct(produceStruct()) != "Struct") throw IllegalStateException()
	if (testClass(produceClass()) != "Class") throw IllegalStateException()
    if (testProtocol(produceProtocol()) != "Protocol") throw IllegalStateException()
	if (testClass2(produceClass()) != "Class") throw IllegalStateException()
	if (testProtocol2(produceProtocol()) != "Protocol") throw IllegalStateException()
}