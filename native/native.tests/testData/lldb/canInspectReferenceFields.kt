// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: canInspectReferenceFields.in
// OUTPUT_DATA_FILE: canInspectReferenceFields.out




fun main(args: Array<String>) {
    val node = Node(1, Node(2, null))
    return
}

data class Node(val value: Int, val next: Node?)
