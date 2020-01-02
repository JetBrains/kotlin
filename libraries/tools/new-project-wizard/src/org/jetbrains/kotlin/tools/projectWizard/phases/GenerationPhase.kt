package org.jetbrains.kotlin.tools.projectWizard.phases

enum class GenerationPhase {
    PREPARE, INIT_TEMPLATE, FIRST_STEP, SECOND_STEP, PROJECT_GENERATION, PROJECT_IMPORT

    ;

    companion object {
        val ALL = values().toSet()

        fun startingFrom(firstPhase: GenerationPhase) =
            values().filter { it >= firstPhase }.toSet()
    }
}

