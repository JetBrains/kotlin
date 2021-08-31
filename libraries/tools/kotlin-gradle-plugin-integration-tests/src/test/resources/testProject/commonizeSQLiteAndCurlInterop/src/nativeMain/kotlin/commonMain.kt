import kotlinx.cinterop.toKString
import curl.*
import sqlite.Fts5PhraseIter
import sqlite.sqlite3_initialize

@OptIn(ExperimentalUnsignedTypes::class)
fun useCurl() {
    val curl = curl_easy_init()
    if (curl != null) {
        curl_easy_setopt(curl, CURLOPT_URL, "http://example.com")
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
        val res = curl_easy_perform(curl)
        if (res != CURLE_OK) {
            println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
        }
        curl_easy_cleanup(curl)
    }
}

fun useSqlite(): Fts5PhraseIter {
    sqlite3_initialize()
    TODO()
}
