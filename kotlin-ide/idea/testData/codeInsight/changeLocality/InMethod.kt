// SCOPE: 'fun baa() {'
// SCOPE: '        val list = listOf<String>("Z")'
// SCOPE: '    }'

class Comment {
    fun q() {

    }

    val someValue: String
        get() {
            return "X"
        }

    fun baa() {
        val list = listOf<String>(<selection>"Z"</selection>)
    }
}