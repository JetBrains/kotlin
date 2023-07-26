/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.stubs

import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition

/**
 * Sort class members. If the source file for the class is unknown, just sort using name and descriptor. Otherwise:
 * - all members in the same source file as the class come first (members may come from other source files)
 * - members from the class are sorted using their position in the source file
 * - members from other source files are sorted using their name and descriptor
 *
 * More details: Class methods and fields are currently sorted at serialization (see DescriptorSerializer.sort) and at deserialization
 * (see DeserializedMemberScope.OptimizedImplementation#addMembers). Therefore, the contents of the generated stub files are sorted in
 * incremental builds but not in clean builds.
 * The consequence is that the contents of the generated stub files may not be consistent across a clean build and an incremental
 * build, making the build non-deterministic and dependent tasks run unnecessarily (see KT-40882).
 */
class MembersPositionComparator(val classSource: KotlinPosition?, val memberData: Map<JCTree, MemberData>) :
    Comparator<JCTree> {
    override fun compare(o1: JCTree, o2: JCTree): Int {
        val data1 = memberData.getValue(o1)
        val data2 = memberData.getValue(o2)
        classSource ?: return compareDescriptors(data1, data2)

        val position1 = data1.position
        val position2 = data2.position

        return if (position1 != null && position1.path == classSource.path) {
            if (position2 != null && position2.path == classSource.path) {
                val positionCompare = position1.pos.compareTo(position2.pos)
                if (positionCompare != 0) positionCompare
                else compareDescriptors(data1, data2)
            } else {
                -1
            }
        } else if (position2 != null && position2.path == classSource.path) {
            1
        } else {
            compareDescriptors(data1, data2)
        }
    }

    private fun compareDescriptors(m1: MemberData, m2: MemberData): Int {
        val nameComparison = m1.name.compareTo(m2.name)
        if (nameComparison != 0) return nameComparison
        return m1.descriptor.compareTo(m2.descriptor)
    }
}
class MemberData(val name: String, val descriptor: String, val position: KotlinPosition?)