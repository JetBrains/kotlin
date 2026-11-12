abstract class TypeReference<T>

class Deserializer {
    private val reference = object : TypeReference<Map<String, List<CharSequence>>>() {}

    val publicRef = object : TypeReference<List<String>>() {}
}
