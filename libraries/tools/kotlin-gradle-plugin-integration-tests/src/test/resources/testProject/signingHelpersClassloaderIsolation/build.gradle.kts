import java.security.Security

plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

try {
    this::class.java.classLoader.loadClass("org.bouncycastle.openpgp.operator.PGPContentSignerBuilder")
} catch (e: ClassNotFoundException) {
    println("PGPContentSignerBuilder class not found")
}
println("Security provider is: ${Security.getProvider("BC")}")

