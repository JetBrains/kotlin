/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.tensorflow

import kotlinx.cinterop.*
import platform.posix.size_t
import tensorflow.*

typealias Status = CPointer<TF_Status>
typealias Operation = CPointer<TF_Operation>
typealias Tensor = CPointer<TF_Tensor>

val Status.isOk: Boolean get() = TF_GetCode(this) == TF_OK
val Status.errorMessage: String get() = TF_Message(this)!!.toKString()
fun Status.delete() = TF_DeleteStatus(this)
fun Status.validate() {
    try {
        if (!isOk) {
            throw Error("Status is not ok: $errorMessage")
        }
    } finally {
        delete()
    }
}

inline fun <T> statusValidated(block: (Status) -> T): T {
    val status = TF_NewStatus()!!
    val result = block(status)
    status.validate()
    return result
}

fun scalarTensor(value: Int): Tensor {
    val data = nativeHeap.allocArray<IntVar>(1)
    data[0] = value

    return TF_NewTensor(
            TF_INT32,
            /* dims = */ null,
            /* num_dims = */ 0,
            /* data = */ data,
            /* len = */ IntVar.size.convert(),
            /* deallocator = */ staticCFunction { dataToFree, _, _ -> nativeHeap.free(dataToFree!!.reinterpret<IntVar>()) },
            /* deallocator_arg = */ null
    )!!
}

val Tensor.scalarIntValue: Int get() {
    if (TF_INT32 != TF_TensorType(this) || IntVar.size.convert<size_t>() != TF_TensorByteSize(this)) {
        throw Error("Tensor is not of type int.")
    }
    if (0 != TF_NumDims(this)) {
        throw Error("Tensor is not scalar.")
    }

    return TF_TensorData(this)!!.reinterpret<IntVar>().pointed.value
}


class Graph {
    val tensorflowGraph = TF_NewGraph()!!

    inline fun operation(type: String, name: String, initDescription: (CPointer<TF_OperationDescription>) -> Unit): Operation {
        val description = TF_NewOperation(tensorflowGraph, type, name)!!
        initDescription(description)
        return statusValidated { TF_FinishOperation(description, it)!! }
    }

    fun constant(value: Int, name: String = "scalarIntConstant") = operation("Const", name) { description ->
        statusValidated { TF_SetAttrTensor(description, "value", scalarTensor(value), it) }
        TF_SetAttrType(description, "dtype", TF_INT32)
    }

    fun intInput(name: String = "input") = operation("Placeholder", name) { description ->
        TF_SetAttrType(description, "dtype", TF_INT32)
    }

    fun add(left: Operation, right: Operation, name: String = "add") = memScoped {
        val inputs = allocArray<TF_Output>(2)
        inputs[0].apply { oper = left; index = 0 }
        inputs[1].apply { oper = right; index = 0 }

        operation("AddN", name) { description ->
            TF_AddInputList(description, inputs, 2)
        }
    }

    // TODO set unique operation names
    operator fun Operation.plus(right: Operation) = add(this, right)

    inline fun <T> withSession(block: Session.() -> T): T {
        val session = Session(this)
        try {
            return session.block()
        } finally {
            session.dispose()
        }
    }
}

class Session(val graph: Graph) {
    private val inputs = mutableListOf<TF_Output>()
    private val inputValues = mutableListOf<Tensor>()
    private var outputs = mutableListOf<TF_Output>()
    private val outputValues = mutableListOf<Tensor?>()
    private val targets = listOf<Operation>()

    private fun createNewSession(): CPointer<TF_Session> {
        val options = TF_NewSessionOptions()
        val session = statusValidated { TF_NewSession(graph.tensorflowGraph, options, it)!! }
        TF_DeleteSessionOptions(options)
        return session
    }

    private var tensorflowSession: CPointer<TF_Session>? = createNewSession()

    private fun clearInputValues() {
        for (inputValue in inputValues) {
            TF_DeleteTensor(inputValue)
        }

        inputValues.clear()
    }

    private fun clearOutputValues() {
        for (outputValue in outputValues) {
            if (outputValue != null)
                TF_DeleteTensor(outputValue)
        }
        outputValues.clear()
    }

    fun dispose() {
        clearInputValues()
        clearOutputValues()
        clearInputs()
        clearOutputs()

        if (tensorflowSession != null) {
            statusValidated { TF_CloseSession(tensorflowSession, it) }
            statusValidated { TF_DeleteSession(tensorflowSession, it) }
            tensorflowSession = null
        }
    }

    private fun setInputsWithValues(inputsWithValues: List<Pair<Operation, Tensor>>) {
        clearInputValues()
        clearInputs()
        for ((input, inputValue) in inputsWithValues) {
            this.inputs.add(nativeHeap.alloc<TF_Output>().apply { oper = input; index = 0 })
            inputValues.add(inputValue)
        }
    }

    private fun setOutputs(outputs: List<Operation>) {
        clearOutputValues()
        clearOutputs()
        this.outputs = outputs.map { nativeHeap.alloc<TF_Output>().apply { oper = it; index = 0 } }.toMutableList()
    }

    private fun clearOutputs() {
        this.outputs.forEach { nativeHeap.free(it) }
        this.outputs.clear()
    }

    private fun clearInputs() {
        this.inputs.forEach { nativeHeap.free(it) }
        this.inputs.clear()
    }

    operator fun invoke(outputs: List<Operation>, inputsWithValues: List<Pair<Operation, Tensor>> = listOf()): List<Tensor?> {
        setInputsWithValues(inputsWithValues)
        setOutputs(outputs)

        return invoke()
    }

    operator fun invoke(output: Operation, inputsWithValues: List<Pair<Operation, Tensor>> = listOf()) =
            invoke(listOf(output), inputsWithValues).single()!!

    operator fun invoke(): List<Tensor?> {
        if (inputs.size != inputValues.size) {
            throw Error("Call SetInputs() before Run()")
        }
        clearOutputValues()

        val inputsCArray = if (inputs.any()) nativeHeap.allocArray<TF_Output>(inputs.size) else null

        inputs.forEachIndexed { i, input ->
            inputsCArray!![i].apply {
                oper = input.oper
                index = input.index
            }
        }

        val outputsCArray = if (outputs.any()) nativeHeap.allocArray<TF_Output>(outputs.size) else null

        outputs.forEachIndexed { i, output ->
            outputsCArray!![i].apply {
                oper = output.oper
                index = output.index
            }
        }

        memScoped {
            val outputValuesCArray = allocArrayOfPointersTo<TF_Tensor>(outputs.map { null })

            statusValidated {
                TF_SessionRun(tensorflowSession, null,
                        inputsCArray, inputValues.toCValues(), inputs.size,
                        outputsCArray, outputValuesCArray, outputs.size,
                        targets.toCValues(), targets.size,
                        null, it)
            }

            for (index in outputs.indices) {
                outputValues.add(outputValuesCArray[index])
            }
        }

        clearInputValues()

        return outputValues
    }
}

fun main() {
    println("Hello, TensorFlow ${TF_Version()!!.toKString()}!")

    val result = Graph().run {
        val input = intInput()

        withSession { invoke(input + constant(2), inputsWithValues = listOf(input to scalarTensor(3))).scalarIntValue }
    }

    println("3 + 2 is $result.")
}