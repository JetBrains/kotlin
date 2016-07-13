/**
 * Created by user on 7/8/16.
 */

interface Message {
    fun writeTo(output: CodedOutputStream)
    fun getBuilder() : Builder

    //TODO: think about something similar to static method getDefaultInstance()

    interface Builder {
        fun build(): Message
    }
}
