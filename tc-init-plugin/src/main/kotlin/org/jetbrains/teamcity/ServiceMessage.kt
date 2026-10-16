package org.jetbrains.teamcity

class ServiceMessage(
    val name: String,
    val attributes: MutableMap<String, String?> = mutableMapOf()
) {
    companion object {
        val IGNORED_TAGS: List<String> = listOf("tc:tags", "flowId", "parent")
        const val ESCAPE_CHARACTER: Char = '|'
        const val START_MESSAGE: String = "##teamcity["
        const val END_MESSAGE: String = "]"
    }

    fun withFlowId(flowId: String?): ServiceMessage {
        if (flowId != null) {
            attributes["flowId"] = flowId
        }
        return this
    }

    fun asString(): String {
        val msg = StringBuilder(START_MESSAGE).append(name)
        if (attributes.isEmpty()) {
            // No attributes
        } else if (attributes[""] != null) {
            appendAttribute(msg, null, attributes[""])
        } else {
            val tags = attributes["tc:tags"]
            if (tags != null) appendAttribute(msg, "tc:tags", tags)
            val flowId = attributes["flowId"]
            val parent = attributes["parent"]
            for ((key, value) in attributes) {
                if (!IGNORED_TAGS.contains(key)) {
                    appendAttribute(msg, key, value)
                }
            }
            if (flowId != null) appendAttribute(msg, "flowId", flowId)
            if (parent != null) appendAttribute(msg, "parent", parent)
        }
        return msg.append(END_MESSAGE).toString()
    }

    private fun appendAttribute(msg: StringBuilder, key: String?, value: Any?): StringBuilder {
        val strValue = value?.toString()
        if (key == null) {
            msg.append(" '")
        } else {
            msg.append(' ').append(key).append("='")
        }
        if (strValue != null) {
            for (i in 0 until strValue.length) {
                val ch = strValue[i]
                when (ch) {
                    '\n' -> msg.append(ESCAPE_CHARACTER).append('n')
                    '\r' -> msg.append(ESCAPE_CHARACTER).append('r')
                    '|' -> msg.append(ESCAPE_CHARACTER).append('|')
                    '\'' -> msg.append(ESCAPE_CHARACTER).append('\'')
                    '[' -> msg.append(ESCAPE_CHARACTER).append('[')
                    ']' -> msg.append(ESCAPE_CHARACTER).append(']')
                    else -> {
                        if (ch.code < 128) {
                            msg.append(ch)
                        } else {
                            msg.append(ESCAPE_CHARACTER).append(String.format("0x%04x", ch.code))
                        }
                    }
                }
            }
        }
        return msg.append('\'')
    }
}
