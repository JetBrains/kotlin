package net


interface Handler {

    fun execute(bytesFromClient: ByteArray): ByteArray

}