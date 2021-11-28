package lab2

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class SomeObj(
    val hidden: Boolean,
    val names: List<String>
)

fun main() {

    val json = """
        {
            "hidden": true,
            "names": ["a", "b"]
        }
    """.trimIndent()

    val moshi: Moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(SomeObj::class.java)
    println(adapter.fromJson(json))
}
