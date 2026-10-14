/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs

import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl

/**
 * A [NodeJsToolchainService] that disables Node.js toolchain.
 *
 * Without a Node.js toolchain, Node.js is configured and provisioned per project, using the Node.js environment spec
 * and the Node.js setup task, instead of Gradle toolchain resolution:
 *
 * - If `download` is enabled, the setup task resolves the Node.js distribution for the current platform and architecture
 *   as an `org.nodejs:node` dependency from a project-level configuration and repository,
 *   unpacks it into the installation directory, and the `node` executable from there is used.
 * - If `download` is disabled, no distribution is downloaded;
 *   the Node.js executable already installed on the machine is used (the `command` from the spec, `node` by default).
 * - Provisioning is not shared between builds or projects through the toolchain mechanism;
 *   each project resolves its Node.js through its own configuration and repositories.
 *
 * Selected with [NodeJsToolchainManagementDsl.disable].
 */
@ExperimentalNodeJsToolchainDsl
interface DisabledNodeJsToolchainService : NodeJsToolchainService<NodeJsToolchainService.Parameters>
