package org.konan.libcurl

import kotlinx.cinterop.*
import libcurl.*

class CUrl(val url: String)  {
    val repositoryIndex = repository.run {
        val index = size
        add(this@CUrl)
        index
    }

    val curl = curl_easy_init();

    init {
        curl_easy_setopt(curl, CURLOPT_URL, url)
        val header: CPointer<header_callback> = staticCFunction(::header_callback)
        curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, header)
        curl_easy_setopt(curl, CURLOPT_HEADERDATA, interpretCPointer<CPointed>(nativeNullPtr + 1 + repositoryIndex.toLong()))
    }

    val header = Event<String>()
    val data = Event<String>()

    fun fetch() {
        val res = curl_easy_perform(curl)
        if (res != CURLE_OK)
            println("curl_easy_perform() failed: ${curl_easy_strerror(res)}")
    }

    fun close() {
        curl_easy_cleanup(curl)
        // Cannot remove urls from repository because it would change other indices
        // We could potentially remove it and update all indices in tails
        repository[repositoryIndex] = null
    }

    companion object {
        val repository = ArrayList<CUrl?>()
    }
}

fun CPointer<ByteVar>.toKString(length: Int): String {
    val bytes = ByteArray(length)
    nativeMemUtils.getByteArray(pointed, bytes, length)
    return kotlin.text.fromUtf8Array(bytes, 0, bytes.size)
}

fun header_callback(buffer: CPointer<ByteVar>?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t {
    if (buffer == null) return 0
    val header = buffer.toKString((size * nitems).toInt()).trim()
    if (userdata != null) {
        val id = userdata.rawValue.toLong() - 1
        val curl = CUrl.repository[id.toInt()]
        if (curl != null)
            curl.header(header)
    }
    return size * nitems
}

/*
fun write_callback(buffer: COpaquePointer?, size: size_t, nitems: size_t, userdata: COpaquePointer?): size_t {
    if (buffer == null) return 0
    if (userdata != null) {
        val id = userdata.rawValue.toLong() - 1
        val curl = CUrl.repository[id.toInt()]
        if (curl != null)
            curl.data(buffer.)
    }
    return size * nitems
}
*/
