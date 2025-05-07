/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.bouncycastle.bcpg.*
import org.bouncycastle.bcpg.sig.Features
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.jcajce.provider.asymmetric.edec.KeyPairGeneratorSpi
import org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_BOUNCY_CASTLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.SecureRandom
import java.util.*
import javax.inject.Inject

@DisableCachingByDefault(because = "PGP keys are not supposed to be cached. This task is intended for CLI usage.")
abstract class GeneratePgpKeys : DefaultTask() {
    @get:Input
    @get:Option(
        option = "name",
        description = "The name to be used for the generated key. Usually in the form of: 'Name Surname <email>'"
    )
    @get:Optional
    abstract val keyName: Property<String>

    @get:Input
    @get:Option(
        option = "password",
        description = "The password to be used for key encryption. If not provided, the value of the 'signing.password' Gradle property will be used."
    )
    @get:Optional
    abstract val password: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Classpath
    abstract val bouncyCastleClasspath: ConfigurableFileCollection

    @get:Input
    abstract val gradleHomePath: Property<String>

    @TaskAction
    fun execute() {
        val files = listOf("secret.gpg", "secret.asc", "public.gpg", "public.asc", "example.properties")
        files.forEach {
            require(
                outputDirectory.get().asFile.resolve(it).exists().not()
            ) {
                """
                    The output directory '${outputDirectory.get().asFile.absoluteFile}' already contains a file named '$it'.
                    Please move your existing key files to another location then try again.                    
                """.trimIndent()
            }
        }

        require(keyName.isPresent) {
            """You must provide a value for the '--name' command line option, e.g. --name "Jane Doe <janedoe@example.com>""""
        }

        require(password.isPresent) {
            "You must provide a value for either the '--password' command line option or the 'signing.password' Gradle property "
        }

        val workQueue: WorkQueue = workerExecutor.classLoaderIsolation {
            it.classpath.from(bouncyCastleClasspath)
        }

        workQueue.submit(GenerateKeys::class.java, Action { parameters ->
            parameters.outputDirectory.set(outputDirectory)
            parameters.keyName.set(keyName)
            parameters.password.set(password.get())
            parameters.gradleHomePath.set(this@GeneratePgpKeys.gradleHomePath)
        })
    }

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    internal interface GenerateKeyParameters : WorkParameters {
        val outputDirectory: DirectoryProperty
        val keyName: Property<String>
        val password: Property<String>
        val gradleHomePath: Property<String>
    }

    internal abstract class GenerateKeys : WorkAction<GenerateKeyParameters> {
        override fun execute() {
            val secretKeys =
                generateKeyRing(parameters.keyName.get(), parameters.password.get().toCharArray()) as PGPSecretKeyRing
            val dir = parameters.outputDirectory.asFile.get()
            FileOutputStream(dir.resolve("secret.gpg")).use { secretOut ->
                secretKeys.encode(secretOut)
            }
            ArmoredOutputStream(FileOutputStream(dir.resolve("secret.asc"))).use { secretOut ->
                secretKeys.encode(secretOut)
            }
            val publicKeys = PGPPublicKeyRing(secretKeys.publicKeys.asSequence().toList())
            FileOutputStream(dir.resolve("public.gpg")).use { publicOut ->
                publicKeys.encode(publicOut)
            }
            ArmoredOutputStream(FileOutputStream(dir.resolve("public.asc"))).use { publicOut ->
                publicKeys.encode(publicOut)
            }

            val keyId = publicKeys.single { it.isMasterKey }.keyID.toULong().toString(16).takeLast(8).uppercase()
            val exampleProperties = """
                signing.keyId=$keyId
                signing.password=<YOUR_PASSWORD>
                signing.secretKeyRingFile=/PATH/TO/secret.gpg
            """.trimIndent()

            dir.resolve("example.properties").writeText(exampleProperties)

            println(
                """
                Generated PGP keys and associated metadata in '${dir.absolutePath}'
                Please move your generated key files to a secure location and do not share them or your password with others.
                The key ID of the generated key is '$keyId'.
                You can use this key ID to configure signing in your build script.
                
                For example, put the following in ${parameters.gradleHomePath.get()}/gradle.properties:
                
                $exampleProperties
                
                You can also find the armored ASCII version of the generated keys in the 'public.asc' and 'secret.asc' files.
                
                To upload your key to a PGP keyserver, you can use:
                
                gradlew uploadPublicPgpKey
            """.trimIndent()
            )
        }

        private fun generateKeyRing(
            identity: String,
            password: CharArray?,
        ): Any {
            val sha1Calc = JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1)
            val eddsaGen = KeyPairGeneratorSpi.EdDSA()
            val xdhGen = KeyPairGeneratorSpi.XDH()
            val random = SecureRandom()

            val contentSignerBuilder: PGPContentSignerBuilder =
                JcaPGPContentSignerBuilder(PublicKeyAlgorithmTags.EDDSA_LEGACY, HashAlgorithmTags.SHA512)
            val secretKeyEncryptor = JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha1Calc)
                .build(password)

            val now = Date()

            eddsaGen.initialize(ECNamedCurveGenParameterSpec("ed25519"), random)
            val primaryKP: KeyPair = eddsaGen.generateKeyPair()
            val primaryKey: PGPKeyPair = JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PGPPublicKey.EDDSA_LEGACY, primaryKP, now)
            val primarySubpackets = PGPSignatureSubpacketGenerator()
            primarySubpackets.setKeyFlags(true, KeyFlags.CERTIFY_OTHER)
            primarySubpackets.setPreferredHashAlgorithms(
                false, intArrayOf(
                    HashAlgorithmTags.SHA512, HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA256, HashAlgorithmTags.SHA224
                )
            )
            primarySubpackets.setPreferredSymmetricAlgorithms(
                false, intArrayOf(
                    SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.AES_192, SymmetricKeyAlgorithmTags.AES_128
                )
            )
            primarySubpackets.setPreferredCompressionAlgorithms(
                false, intArrayOf(
                    CompressionAlgorithmTags.ZLIB,
                    CompressionAlgorithmTags.UNCOMPRESSED
                )
            )
            primarySubpackets.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION)
            primarySubpackets.setIssuerFingerprint(false, primaryKey.publicKey)

            eddsaGen.initialize(ECNamedCurveGenParameterSpec("ed25519"), random)
            val signingKP: KeyPair = eddsaGen.generateKeyPair()
            val signingKey: PGPKeyPair = JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PGPPublicKey.EDDSA_LEGACY, signingKP, now)
            val signingKeySubpacket = PGPSignatureSubpacketGenerator()
            signingKeySubpacket.setKeyFlags(true, KeyFlags.SIGN_DATA)
            signingKeySubpacket.setIssuerFingerprint(false, primaryKey.publicKey)

            xdhGen.initialize(ECNamedCurveGenParameterSpec("X25519"), random)
            val encryptionKP: KeyPair = xdhGen.generateKeyPair()
            val encryptionKey: PGPKeyPair = JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PGPPublicKey.ECDH, encryptionKP, now)
            val encryptionKeySubpackets = PGPSignatureSubpacketGenerator()
            encryptionKeySubpackets.setKeyFlags(true, KeyFlags.ENCRYPT_COMMS or KeyFlags.ENCRYPT_STORAGE)
            encryptionKeySubpackets.setIssuerFingerprint(false, primaryKey.publicKey)

            val gen = PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION, primaryKey, identity,
                sha1Calc, primarySubpackets.generate(), null, contentSignerBuilder, secretKeyEncryptor
            )
            gen.addSubKey(signingKey, signingKeySubpacket.generate(), null, contentSignerBuilder)
            gen.addSubKey(encryptionKey, encryptionKeySubpackets.generate(), null)

            val secretKeys = gen.generateSecretKeyRing()
            return secretKeys
        }
    }

}

@DisableCachingByDefault(because = "Uploading keys to a keyserver is not cacheable. This task is intended for CLI usage.")
abstract class UploadPgpKeyTask : DefaultTask() {
    @get:Input
    @get:Option(
        option = "keyring",
        description = "The file that contains the public key to upload to the keyserver in armored ASCII format. Default: '<PROJECT_DIRECTORY>/gpg/public.asc'"
    )
    @get:Optional
    abstract val keyring: Property<String>

    @get:Input
    @get:Option(
        option = "keyserver",
        description = "The address of the keyserver to upload the key to. Default: 'https://keyserver.ubuntu.com'"
    )
    @get:Optional
    abstract val keyserver: Property<String>

    @TaskAction
    fun execute() {
        val publicKeyringFile = File(keyring.get())
        require(publicKeyringFile.isFile) {
            "The provided public keyring file does not exist or cannot be read: ${publicKeyringFile.absolutePath}"
        }
        val publicKeyringContent = publicKeyringFile.readText()
        require(publicKeyringContent.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
            """
                The provided public keyring file does not start with '-----BEGIN PGP PUBLIC KEY BLOCK-----'.
                Please make sure that the provided file contains a valid public key in armored ASCII format.
            """.trimIndent()
        }
        val url = URL("${keyserver.get()}/pks/add")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            connection.doInput = true
            connection.allowUserInteraction = true

            connection.outputStream.writer().buffered().use {
                it.write("keytext=")
                it.write(URLEncoder.encode(publicKeyringContent, StandardCharsets.UTF_8.toString()))

                // nm stands for no modification, as described in the HKP protocol documentation:
                // https://www.ietf.org/archive/id/draft-gallagher-openpgp-hkp-04.html#name-the-nm-no-modification-opti
                it.write("&options=")
                it.write(URLEncoder.encode("nm", StandardCharsets.UTF_8.toString()))
            }

            val result = connection.inputStream.reader().use { it.readText() }
            println("Key upload successful. Server returned:\n$result")

        } catch (e: IOException) {
            connection.errorStream?.reader()?.use { it.readText() }?.also { result ->
                println("Failed to upload public key. Server returned:\n$result")
            }
            throw e
        } finally {
            connection.disconnect()
        }
    }
}

internal fun Project.addPgpSignatureHelpers() {
    project
        .configurations
        .maybeCreateResolvable(KOTLIN_BOUNCY_CASTLE_CONFIGURATION_NAME)
        .defaultDependencies {
            it.add(
                project.dependencies.create("org.bouncycastle:bcpkix-jdk18on:1.80")
            )
            it.add(
                project.dependencies.create("org.bouncycastle:bcpg-jdk18on:1.80")
            )
        }

    val gpgDirectory = project.layout.buildDirectory.dir("pgp")
    project.tasks.register("generatePgpKeys", GeneratePgpKeys::class.java) {
        it.notCompatibleWithConfigurationCache("Do not cache password.")
        it.outputs.upToDateWhen { false }
        it.outputDirectory.set(gpgDirectory)
        it.password.set(project.providers.gradleProperty("signing.password"))
        it.bouncyCastleClasspath.from(project.configurations.named(KOTLIN_BOUNCY_CASTLE_CONFIGURATION_NAME))
        it.gradleHomePath.set(project.gradle.gradleUserHomeDir.absolutePath)
        it.group = "signing"
        it.description = """
            Generates a new PGP keypair.
            
            Usage: 
            gradlew generatePgpKeys --name "Jane Doe <janedoe@example.com>" --password YOUR_PASSWORD
        """.trimIndent()
    }

    project.tasks.register("uploadPublicPgpKey", UploadPgpKeyTask::class.java) {
        it.keyring.set(gpgDirectory.map { dir -> dir.file("public.asc").asFile.absolutePath })
        it.keyserver.set("https://keyserver.ubuntu.com")
        it.group = "signing"
        it.description = "Uploads the public PGP key to a keyserver"
    }
}