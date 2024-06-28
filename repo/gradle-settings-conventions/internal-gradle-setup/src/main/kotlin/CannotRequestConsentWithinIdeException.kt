/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.gradle.api.GradleException

class CannotRequestConsentWithinIdeException(consentDetailsLink: String?) : GradleException(
    """
    |$USER_CONSENT_REQUEST
    |The consent cannot be requested in interactive mode when running from IDE.
    |Please either invoke Gradle from the command line or add -P$CONSENT_DECISION_GRADLE_PROPERTY=(true,false) to the run parameters in order to make a decision
    |${if (consentDetailsLink != null) USER_CONSENT_DETAILS_LINK_TEMPLATE.formatWithLink(consentDetailsLink) else ""}
    """.trimMargin()
)