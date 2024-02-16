/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata

import kotlin.metadata.internal.FlagImpl
import org.jetbrains.kotlin.metadata.deserialization.Flags as ProtoFlags
import org.jetbrains.kotlin.metadata.ProtoBuf.Class.Kind as ProtoClassKind
import org.jetbrains.kotlin.metadata.ProtoBuf.Visibility as ProtoVisibility
import org.jetbrains.kotlin.metadata.ProtoBuf.Modality as ProtoModality
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind as ProtoMemberKind

// Pay attention to the order of enums in this file!
// Order of enum values is directly linked to order of corresponding protobuf enums in org.jetbrains.kotlin.metadata.ProtoBuf.
// Changes in the binary format should be reflected in the protobuf and therefore in enums in this file.
// Arbitrary reordering of enum members here will likely break deserialization.

/**
 * Represents visibility level (also known as access level) of the corresponding declaration.
 * Some of these visibilities may be non-denotable in Kotlin.
 */
public enum class Visibility(kind: Int) {
    /**
     * Signifies that the corresponding declaration is `internal`.
     */
    INTERNAL(ProtoVisibility.INTERNAL_VALUE),

    /**
     * Signifies that the corresponding declaration is `private`.
     */
    PRIVATE(ProtoVisibility.PRIVATE_VALUE),

    /**
     * Signifies that the corresponding declaration is `protected`.
     */
    PROTECTED(ProtoVisibility.PROTECTED_VALUE),

    /**
     * Signifies that the corresponding declaration is `public`.
     */
    PUBLIC(ProtoVisibility.PUBLIC_VALUE),

    /**
     * Signifies that the corresponding declaration is "private-to-this", which is a non-denotable visibility of
     * private members in Kotlin which are callable only on the same instance of the declaring class.
     * Generally, this visibility is more restrictive than 'private', so for most use cases it can be treated the same.
     *
     * Example of 'PRIVATE_TO_THIS' declaration:
     * ```
     *  class A<in T>(t: T) {
     *      private val t: T = t // visibility for t is PRIVATE_TO_THIS
     *
     *      fun test() {
     *          val x: T = t // correct
     *          val y: T = this.t // also correct
     *      }
     *      fun foo(a: A<String>) {
     *         val x: String = a.t // incorrect, because a.t can be Any
     *      }
     *  }
     *  ```
     */
    PRIVATE_TO_THIS(ProtoVisibility.PRIVATE_TO_THIS_VALUE),

    /**
     * Signifies that the corresponding declaration is local, i.e., declared inside a code block,
     * and not visible from the outside.
     */
    LOCAL(ProtoVisibility.LOCAL_VALUE)
    ;

    internal val flag = FlagImpl(ProtoFlags.VISIBILITY, kind)
}

/**
 * Represents modality of the corresponding declaration.
 *
 * Modality determines when and where it is possible to extend/override a class/member.
 */
public enum class Modality(kind: Int) {
    /**
     * Signifies that the corresponding declaration is `final`.
     */
    FINAL(ProtoModality.FINAL_VALUE),

    /**
     * Signifies that the corresponding declaration is `open`.
     */
    OPEN(ProtoModality.OPEN_VALUE),

    /**
     * Signifies that the corresponding declaration is `abstract`.
     */
    ABSTRACT(ProtoModality.ABSTRACT_VALUE),

    /**
     * Signifies that the corresponding declaration is `sealed`.
     *
     * Pay attention that this modality is not applicable to class members.
     * Setting it as a value for member modality leads to an undefined behavior.
     */
    SEALED(ProtoModality.SEALED_VALUE)
    ;

    internal val flag = FlagImpl(ProtoFlags.MODALITY, kind)
}

/**
 * Represents the kind of the corresponding class, i.e., the way it is declared in the source code.
 */
public enum class ClassKind(kind: Int) {
    /**
     * Signifies that the corresponding class is a usual or anonymous class.
     */
    CLASS(ProtoClassKind.CLASS_VALUE),

    /**
     * Signifies that the corresponding class is an `interface`.
     */
    INTERFACE(ProtoClassKind.INTERFACE_VALUE),

    /**
     * Signifies that the corresponding class is an `enum class`.
     */
    ENUM_CLASS(ProtoClassKind.ENUM_CLASS_VALUE),

    /**
     * Signifies that the corresponding class is an enum entry.
     */
    ENUM_ENTRY(ProtoClassKind.ENUM_ENTRY_VALUE),

    /**
     * Signifies that the corresponding class is an `annotation class`.
     */
    ANNOTATION_CLASS(ProtoClassKind.ANNOTATION_CLASS_VALUE),

    /**
     * Signifies that the corresponding class is a non-companion, singleton `object`.
     */
    OBJECT(ProtoClassKind.OBJECT_VALUE),

    /**
     * Signifies that the corresponding class is a `companion object`.
     */
    COMPANION_OBJECT(ProtoClassKind.COMPANION_OBJECT_VALUE)
    ;

    internal val flag = FlagImpl(ProtoFlags.CLASS_KIND, kind)
}

/**
 * Represents kind of a function or property.
 *
 * Kind indicates the origin of a declaration within a containing class.
 * It provides information about whether a function or property was defined, generated, or something else.
 */
public enum class MemberKind(kind: Int) {
    /**
     * Signifies that the corresponding function or property is explicitly declared in the containing class.
     */
    DECLARATION(ProtoMemberKind.DECLARATION_VALUE),

    /**
     * Signifies that the corresponding function or property exists in the containing class because a function with a suitable
     * signature exists in a supertype.
     * This flag is not written by the Kotlin compiler and normally cannot be encountered in binary metadata.
     * Its effects are unspecified.
     */
    FAKE_OVERRIDE(ProtoMemberKind.FAKE_OVERRIDE_VALUE),

    /**
     * Signifies that the corresponding function or property exists in the containing class because it has been produced
     * by interface delegation.
     *
     * Not to be confused with property delegation which is denoted by [KmProperty.isDelegated].
     */
    DELEGATION(ProtoMemberKind.DELEGATION_VALUE),

    /**
     * Signifies that the corresponding function or property exists in the containing class because it has been synthesized
     * by the compiler or compiler plugin and has no declaration in the source code.
     *
     * An example of such function can be component1() of a data class.
     */
    SYNTHESIZED(ProtoMemberKind.SYNTHESIZED_VALUE)
    ;

    internal val flag = FlagImpl(ProtoFlags.MEMBER_KIND, kind)
}
