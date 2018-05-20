/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools

class ClassVisibility(val name: String, val visibility: String?, val members: Map<MemberSignature, MemberVisibility>, val isCompanion: Boolean = false, val facadeClassName: String? = null) {
    var companionVisibilities: ClassVisibility? = null
    val partVisibilities = mutableListOf<ClassVisibility>()
}

fun ClassVisibility.findMember(signature: MemberSignature): MemberVisibility? =
    members[signature] ?: partVisibilities.mapNotNull { it.members[signature] }.firstOrNull()


data class MemberVisibility(val member: MemberSignature, val declaration: String?, val visibility: String?)
data class MemberSignature(val name: String, val desc: String)

private fun isPublic(visibility: String?, isPublishedApi: Boolean) = visibility == null || visibility == "public" || visibility == "protected"  || (isPublishedApi && visibility == "internal")
fun ClassVisibility.isPublic(isPublishedApi: Boolean) = isPublic(visibility, isPublishedApi)
fun MemberVisibility.isPublic(isPublishedApi: Boolean) = isPublic(visibility, isPublishedApi)




