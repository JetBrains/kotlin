package templates

import templates.GenericFunction.TypeParameter
import templates.GenericFunction.TypeParameter.*

fun parseTypeParameter(typeString: String): TypeParameter =
    removeAnnotations(typeString.trim().removePrefix("reified ")).let { trimmed ->
        if (':' in trimmed) {
            val (name, constraint) = trimmed.split(':')
            TypeParameter(typeString, name.trim(), parseTypeRef(removeAnnotations(constraint.trim())))
        } else {
            TypeParameter(typeString, trimmed)
        }
    }

fun parseTypeRef(typeRef: String): TypeRef =
    typeRef.trim().run {
        if (contains('<') && endsWith('>')) {
            val name = substringBefore('<')
            val params = substringAfter('<').substringBeforeLast('>')
            TypeRef(name, parseArguments(params))
        }
        else
            TypeRef(this)
    }

private fun parseTypeArgument(typeParam: String): TypeArgument
    = typeParam.trim().removePrefix("in ").removePrefix("out ").let { TypeArgument(parseTypeRef(it)) }


private fun parseArguments(typeParams: String): List<TypeArgument> {
    var restParams: String = typeParams
    val params = mutableListOf<TypeArgument>()
    while (true) {
        val comma = restParams.indexOf(',')
        if (comma < 0) {
            params += parseTypeArgument(restParams)
            break
        } else {
            val open = restParams.indexOf('<')
            val close = restParams.indexOf('>')
            if (comma !in open..close) {
                params += parseTypeArgument(restParams.take(comma))
                restParams = restParams.drop(comma + 1)
            }
            else {
                params += parseTypeArgument(restParams.take(close + 1))
                val nextComma = restParams.indexOf(',', startIndex = close)
                if (nextComma < 0) break
                restParams = restParams.drop(nextComma + 1)
            }
        }
    }
    return params
}

private fun removeAnnotations(typeParam: String) =
    typeParam.replace("""^(@[\w\.]+\s+)+""".toRegex(), "")

