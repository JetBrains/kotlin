/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A block-level KDoc/Javadoc tag, e.g. `@param x description`. */
@Serializable
sealed class KdDocTag

/** A Javadoc/KDoc `@author` tag. */
@Serializable
@SerialName("authorTag")
data class KdDocAuthorTag(val root: KdDocContentNode) : KdDocTag()

/** A Javadoc/KDoc `@since` tag. */
@Serializable
@SerialName("sinceTag")
data class KdDocSinceTag(val root: KdDocContentNode) : KdDocTag()

/** A Javadoc/KDoc `@see` tag. */
@Serializable
@SerialName("seeTag")
data class KdDocSeeTag(
    val root: KdDocContentNode,
    val subject: String,
    /** `null` if [subject] could not be resolved. */
    val address: KdElementId?,
) : KdDocTag()

/** A KDoc/Javadoc `@param` tag documenting a value or type parameter named [name]. */
@Serializable
@SerialName("paramTag")
data class KdDocParamTag(
    val root: KdDocContentNode,
    val name: String,
    val address: KdElementId,
) : KdDocTag()

/** A KDoc/Javadoc `@return` tag. */
@Serializable
@SerialName("returnTag")
data class KdDocReturnTag(val root: KdDocContentNode) : KdDocTag()

/** A KDoc `@receiver` tag. */
@Serializable
@SerialName("receiverTag")
data class KdDocReceiverTag(val root: KdDocContentNode) : KdDocTag()

/** A KDoc `@constructor` tag. */
@Serializable
@SerialName("constructorTag")
data class KdDocConstructorTag(val root: KdDocContentNode) : KdDocTag()

/** A KDoc/Javadoc `@throws`/`@exception` tag documenting the exception type named [name]. */
@Serializable
@SerialName("throwsTag")
data class KdDocThrowsTag(
    val root: KdDocContentNode,
    val name: String,
    /** `null` if [name] could not be resolved. */
    val address: KdElementId?,
) : KdDocTag()

/** A KDoc `@sample` tag referencing the sample function named [name]. */
@Serializable
@SerialName("sampleTag")
data class KdDocSampleTag(
    val root: KdDocContentNode,
    val name: String,
    /** `null` if [name] could not be resolved. */
    val address: KdElementId?,
) : KdDocTag()

/** A KDoc `@property` tag documenting the primary-constructor property named [name]. */
@Serializable
@SerialName("propertyTag")
data class KdDocPropertyTag(
    val root: KdDocContentNode,
    val name: String,
    /** `null` if [name] could not be resolved. */
    val address: KdElementId?,
) : KdDocTag()

/** A KDoc `@suppress` tag. */
@Serializable
@SerialName("suppressTag")
data class KdDocSuppressTag(val root: KdDocContentNode) : KdDocTag()

/** A Javadoc `@version` tag. */
@Serializable
@SerialName("versionTag")
data class KdDocVersionTag(val root: KdDocContentNode) : KdDocTag()

/** A Javadoc `@deprecated` tag; for the Kotlin `@Deprecated` annotation, see [KdDeprecated]. */
@Serializable
@SerialName("deprecatedTag")
data class KdDocDeprecatedTag(val root: KdDocContentNode) : KdDocTag()

/** A tag not otherwise represented in this model. */
@Serializable
@SerialName("unknownTag")
data class KdDocUnknownTag(val root: KdDocContentNode, val name: String, val content: String) : KdDocTag()
