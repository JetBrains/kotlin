package kotlin.test.tests

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal operator fun String.contains(sub: String) = (this as java.lang.String).contains(sub)

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal fun String.startsWith(other: String) = (this as java.lang.String).startsWith(other)
