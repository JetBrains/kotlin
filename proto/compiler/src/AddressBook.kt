/**
 * Created by user on 7/13/16.
 */
class AddressBook private constructor (people: Array <Person> ) {
    var people : Array <Person>
        private set

    init {
        this.people = people
    }

    fun writeTo (output: CodedOutputStream) {
        writeToNoTag(output)
    }

    fun writeToNoTag (output: CodedOutputStream) {
        if (people.size > 0) {
            output.writeInt32NoTag(people.size)
            for (item in people) {
                item.writeToNoTag(output)
            }
        }
    }

    class BuilderAddressBook constructor (people: Array <Person> ) {
        var people : Array <Person>

        init {
            this.people = people
        }

        fun readFrom (input: CodedInputStream) {
            readFromNoTag(input)
        }

        fun readFromNoTag (input: CodedInputStream) {
            if (people.size > 0) {
                val tag = input.readTag()
                val listSize = input.readInt32NoTag()
                for (i in 1..listSize) {
                    people[i - 1].mergeFrom(input)
                }
            }
        }

        fun build(): AddressBook {
            return AddressBook(people)
        }
    }

    fun mergeFrom (input: CodedInputStream) {
        if (people.size > 0) {
            val tag = input.readTag()
            val listSize = input.readInt32NoTag()
            for (i in 1..listSize) {
                people[i - 1].mergeFrom(input)
            }
        }
    }
}


