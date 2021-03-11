import java.net.URLConnection

fun foo(url<caret>){}

// EXIST_JAVA_ONLY: { itemText: "urlConnection: URLConnection", tailText: " (java.net)" }
// ABSENT: { itemText: "urlconnection: URLConnection" }
