package kotlin.template

/**
 * Creates a string template from a string with $ expressions inside.
 */
// TODO varargs on constructors seems to fail
//class StringTemplate(vararg val text: String) {
open class StringTemplate(val constantText : Array<String>) {

    /**
     * Creates a builder of string expressions
     */
    open fun builder() : StringTemplateBuilder = StringTemplateBuilder(constantText)
}

/**
 * Used to build strings using expressions
 */
open class StringTemplateBuilder(val constantText : Array<String>){
    protected val buffer: StringBuilder = StringBuilder()
    var index : Int = 0

    fun build() : String {
        while (appendNextConstant()) {}
        return buffer.toString() ?: ""
    }

    open fun expression(expression : Any) {
        appendNextConstant()
        buffer.append(expression)
    }

    /**
     * Returns the next static text value from the template
     */
    protected fun appendNextConstant(): Boolean {
        if (index < constantText.size) {
            buffer.append(constantText[index++])
            return true
        } else {
            return false
        }
    }
}