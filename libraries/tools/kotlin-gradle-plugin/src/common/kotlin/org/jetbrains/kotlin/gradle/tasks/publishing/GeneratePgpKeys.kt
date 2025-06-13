/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.publishing

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.CompressionAlgorithmTags
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyPacket
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.Features
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilderProvider
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPairGeneratorProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject

@DisableCachingByDefault(because = "PGP keys are not supposed to be cached. This task is intended for CLI usage.")
abstract class GeneratePgpKeys @Inject internal constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @get:Input
    @get:Option(
        option = "name", description = "The name to be used for the generated key. Usually in the form of: 'Name Surname <email>'"
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

    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val bouncyCastleClasspath: ConfigurableFileCollection

    @get:Internal
    abstract val gradleHomePath: Property<String>

    @TaskAction
    protected fun execute() {
        require(keyName.isPresent) {
            """You must provide a value for the '--name' command line option, e.g. --name "Jane Doe <janedoe@example.com>""""
        }

        require(password.isPresent) {
            "You must provide a value for either the '--password' command line option or the 'signing.password' Gradle property "
        }

        val workQueue: WorkQueue = workerExecutor.classLoaderIsolation {
            it.classpath.from(bouncyCastleClasspath)
        }

        workQueue.submit(GenerateKeys::class.java) { parameters ->
            parameters.outputDirectory.set(outputDirectory)
            parameters.keyName.set(keyName)
            parameters.password.set(password.get())
            parameters.gradleHomePath.set(this@GeneratePgpKeys.gradleHomePath)
        }
    }

    internal interface GenerateKeyParameters : WorkParameters {
        val outputDirectory: DirectoryProperty
        val keyName: Property<String>
        val password: Property<String>
        val gradleHomePath: Property<String>
    }

    internal abstract class GenerateKeys : WorkAction<GenerateKeyParameters> {
        private val PGPPublicKey.keyIdHex get() = keyIdToHex(keyID)

        override fun execute() {
            val secretKeys = generateKeyRing(parameters.keyName.get(), parameters.password.get().toCharArray()) as PGPSecretKeyRing
            val publicKeys = PGPPublicKeyRing(secretKeys.publicKeys.asSequence().toList())
            val keyId = publicKeys.single { it.isMasterKey }.keyIdHex.takeLast(8).uppercase()
            val dir = parameters.outputDirectory.asFile.get()
            val files =
                listOf("secret_$keyId.gpg", "secret_$keyId.asc", "public_$keyId.gpg", "public_$keyId.asc", "example_$keyId.properties")
            files.forEach {
                check(
                    dir.resolve(it).exists().not()
                ) {
                    """
                    The output directory '${dir.absoluteFile}' already contains a file named '$it'.
                    Please move your existing key files to another location then try again.                    
                """.trimIndent()
                }
            }
            if (!dir.exists() && !dir.mkdirs()) {
                error("Failed to create output directory '${dir.absolutePath}'")
            }
            FileOutputStream(dir.resolve("secret_$keyId.gpg")).use { secretOut ->
                secretKeys.encode(secretOut)
            }
            ArmoredOutputStream(FileOutputStream(dir.resolve("secret_$keyId.asc"))).use { secretOut ->
                secretKeys.encode(secretOut)
            }

            FileOutputStream(dir.resolve("public_$keyId.gpg")).use { publicOut ->
                publicKeys.encode(publicOut)
            }
            ArmoredOutputStream(FileOutputStream(dir.resolve("public_$keyId.asc"))).use { publicOut ->
                publicKeys.encode(publicOut)
            }

            val exampleProperties = """
                signing.keyId=$keyId
                signing.password=<YOUR_PASSWORD>
                signing.secretKeyRingFile=/PATH/TO/secret_$keyId.gpg
            """.trimIndent()

            dir.resolve("example_$keyId.properties").writeText(exampleProperties)

            logger.quiet(
                """
                Generated PGP keys and associated metadata in '${dir.absolutePath}'
                Please move your generated key files to a secure location and do not share the secret key or your password with others.
                The key ID of the generated key is '$keyId'.
                You can use this key ID to configure signing in your build script.
                
                For example, put the following in ${parameters.gradleHomePath.get()}${File.separator}gradle.properties:
                
${exampleProperties.prependIndent("                ")}
                
                More information: https://kotl.in/gradle-signing-signatory-credentials
                
                You can also find the armored ASCII version of the generated keys in the 'public_$keyId.asc' and 'secret_$keyId.asc' files.
                
                To upload your key to a PGP keyserver, you can use:
                
                gradlew uploadPublicPgpKey --keyring="${dir.resolve("public_$keyId.asc").absolutePath}"
            """.trimIndent()
            )
        }

        // adapted from the general outline of key pair generation from:
        // https://github.com/bcgit/bc-java/blob/main/pg/src/main/java/org/bouncycastle/openpgp/examples/EllipticCurveKeyPairGenerator.java
        // however, this implementation avoids using the JCA framework to not pollute the `java.security.Security` provider with BC classes
        private fun generateKeyRing(
            identity: String,
            password: CharArray?,
        ): Any {
            val generator = BcPGPKeyPairGeneratorProvider().get(PublicKeyPacket.VERSION_4, Date())
            val primaryKey = generator.generateLegacyEd25519KeyPair()

            val sha1Calc = BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1)
            val contentSignerBuilder: PGPContentSignerBuilder =
                BcPGPContentSignerBuilderProvider(HashAlgorithmTags.SHA512).get(primaryKey.publicKey)
            val secretKeyEncryptor = BcPBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha1Calc).build(password)
            val primarySubpackets = PGPSignatureSubpacketGenerator()
            primarySubpackets.setKeyFlags(true, KeyFlags.SIGN_DATA)
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
                    CompressionAlgorithmTags.BZIP2,
                    CompressionAlgorithmTags.ZIP,
                    CompressionAlgorithmTags.UNCOMPRESSED
                )
            )
            primarySubpackets.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION)
            primarySubpackets.setIssuerFingerprint(false, primaryKey.publicKey)

            val gen = PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                primaryKey,
                identity,
                sha1Calc,
                primarySubpackets.generate(),
                null,
                contentSignerBuilder,
                secretKeyEncryptor
            )

            val secretKeys = gen.generateSecretKeyRing()
            return secretKeys
        }

        private companion object {
            private val logger: Logger = Logging.getLogger(GenerateKeys::class.java)
        }
    }
}