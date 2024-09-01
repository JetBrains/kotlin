package hprof

import java.io.File

fun File.readProfile(): Profile {
    return inputStream().buffered().use { it.readProfile() }
}

fun File.write(profile: Profile) {
    outputStream().buffered().use { it.write(profile) }
}
