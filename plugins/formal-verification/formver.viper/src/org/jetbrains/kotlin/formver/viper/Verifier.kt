/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import org.jetbrains.kotlin.formver.viper.ast.Program
import viper.silicon.Config
import viper.silicon.logger.MemberSymbExLogger
import viper.silicon.logger.`NoopSymbExLog$`
import viper.silicon.logger.SymbExLogger
import viper.silicon.verifier.DefaultMainVerifier
import viper.silver.cfg.silver.SilverCfg
import viper.silver.reporter.StdIOReporter

sealed interface VerifierError {
    val msg: String
}

data class ConsistencyError(val err: viper.silver.verifier.ConsistencyError) : VerifierError {
    override val msg = err.toString()
}

data class VerificationError(val result: viper.silicon.interfaces.VerificationResult) : VerifierError {
    override val msg = result.toString()
}

class Verifier {
    private val verifier: DefaultMainVerifier

    init {
        // Viper requires a file to be passed as part of the configuration, hence we need to specify a
        // dummy file name and also specify that that file should be ignored.
        val config = Config(seqOf("--ignoreFile", "dummy.vpr"))
        @Suppress("UNCHECKED_CAST")
        verifier = DefaultMainVerifier(
            config,
            StdIOReporter("stdout_reporter", true),
            `NoopSymbExLog$`.`MODULE$` as SymbExLogger<out MemberSymbExLogger>,
        )
        verifier.start()
    }

    /** Check whether the Viper program is consistent.  Return true on success.
     */
    fun checkConsistency(program: Program, onFailure: (ConsistencyError) -> Unit): Boolean {
        val viperProgram = program.toViper()
        val consistencyResults = viperProgram.checkTransitively()
        var success = true
        for (result in consistencyResults) {
            onFailure(ConsistencyError(result))
            success = false
        }
        return success
    }

    /** Verify the program.  Returns true on successful verification.
     */
    fun verify(program: Program, onFailure: (VerificationError) -> Unit): Boolean {
        val viperProgram = program.toViper()
        var success = true

        val results = verifier.verify(viperProgram, emptySeq<SilverCfg>(), Option.None<String>().toScala())

        for (result in results) {
            if (result.isFatal) {
                onFailure(VerificationError(result))
                success = false
            }
        }

        return success
    }
}