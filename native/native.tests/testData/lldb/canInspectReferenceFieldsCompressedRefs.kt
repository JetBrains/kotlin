// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xbinary=compressedRefs=true
// INPUT_DATA_FILE: canInspectReferenceFieldsCompressedRefs.in
// OUTPUT_DATA_FILE: canInspectReferenceFieldsCompressedRefs.out



fun main(args: Array<String>) {
    val node = Node(1, Node(2, null))
    return
}

data class Node(val value: Int, val next: Node?)
