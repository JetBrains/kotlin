package hprof

fun Profile.normalize(): Profile =
        copy(records = records.map { it.normalize() })

private fun Profile.Record.normalize(): Profile.Record =
        when (this) {
            is HeapDump -> copy(records = records.sortedBy { it.classOrder() })
            is HeapDumpSection -> copy(records = records.sortedBy { it.classOrder() })
            HeapDumpEnd -> this
            is LoadClass -> this
            is StackFrame -> this
            is StackTrace -> this
            is StartThread -> this
            is StringConstant -> this
            is UnknownRecord -> this
        }

private fun HeapDump.Record.classOrder(): Int =
        when (this) {
            is ClassDump -> 1
            is InstanceDump -> 2
            is ObjectArrayDump -> 3
            is PrimitiveArrayDump -> 4
            is RootJavaFrame -> 5
            is RootJniGlobal -> 6
            is RootJniLocal -> 7
            is RootStickyClass -> 8
            is RootThreadObject -> 9
            is RootUnknown -> 10
        }
