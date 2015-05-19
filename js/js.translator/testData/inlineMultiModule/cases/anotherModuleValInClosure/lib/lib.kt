package utils

public var LOG: String = ""

inline
public fun log(s: String): String {
    LOG += s
    return LOG
}
