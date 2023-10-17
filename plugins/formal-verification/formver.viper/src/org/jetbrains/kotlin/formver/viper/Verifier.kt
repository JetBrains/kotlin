/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.errors.ConsistencyError
import org.jetbrains.kotlin.formver.viper.errors.ErrorAdapter
import org.jetbrains.kotlin.formver.viper.errors.GenericConsistencyError
import org.jetbrains.kotlin.formver.viper.errors.VerificationError
import viper.silicon.Config
import viper.silicon.logger.MemberSymbExLogger
import viper.silicon.logger.`NoopSymbExLog$`
import viper.silicon.logger.SymbExLogger
import viper.silicon.verifier.DefaultMainVerifier
import viper.silver.cfg.silver.SilverCfg
import viper.silver.reporter.StdIOReporter


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
    }

    /**
     * Check whether the Viper program is consistent.  Return true on success.
     */
    fun checkConsistency(program: Program, onFailure: (ConsistencyError) -> Unit): Boolean {
        val viperProgram = program.toSilver()
        val consistencyResults = viperProgram.checkTransitively()
        var success = true
        for (result in consistencyResults) {
            onFailure(GenericConsistencyError(result))
            success = false
        }
        return success
    }

    /**
     * Verify the program. Returns true on successful verification.
     */
    fun verify(program: Program, onFailure: (VerificationError) -> Unit): Boolean {
        val viperProgram = program.toSilver()

        verifier.start()
        val results = verifier.verify(viperProgram, emptySeq<SilverCfg>(), null.toScalaOption<String?>())

        var success = true
        for (result in results) {
            if (result.isFatal) {
                onFailure(ErrorAdapter.translate(result))
                success = false
            }
        }
        return success
    }
}