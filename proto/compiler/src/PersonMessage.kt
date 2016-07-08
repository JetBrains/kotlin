/**
 * Created by user on 7/8/16.
 */

// We don't use Message interface for a moment because of LLVM-translator restrictions
class PersonMessage(val name: String, val id: Long, val hasCat: Boolean) // : Message
{
    fun writeTo(output: CodedOutputStream) {
        output.writeString(1, name)
        output.writeInt64(2, id)
        output.writeBool(3, hasCat)
    }

    fun readFrom(input: CodedInputStream) : PersonMessage {
        val newName = input.readString().value
        val newId = input.readInt64().value
        val newHasCatFlag = input.readBool().value
        return PersonMessage(newName, newId, newHasCatFlag)
    }

    fun getBuilder(): PersonBuilder {
        return PersonBuilder()
    }

    // No interface, see above
    class PersonBuilder { // : Message.Builder
        // TODO: think how can we
        var name_: String = ""
        var id_: Long = 0
        var hasCat_: Boolean = false

        fun setName(name: String) {
            name_ = name
        }

        fun setId(id: Long) {
            id_ = id
        }

        fun setHasCat(hasCat: Boolean) {
            hasCat_ = hasCat
        }

        fun build(): PersonMessage {
            return PersonMessage(name_, id_, hasCat_) // Java implementation caches such instances
        }
    }
}