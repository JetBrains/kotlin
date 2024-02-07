/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.CValue

internal fun findObjCCategoriesInSameFilesAsClasses(classCursors: List<CValue<CXCursor>>): List<CValue<CXCursor>> {
    val fileToClassNames = mutableMapOf<CXFile, MutableSet<String>>()
    for (cursor in classCursors) {
        check(cursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl) { cursor.kind }
        val file = getContainingFile(cursor) ?: continue
        val name = clang_getCursorDisplayName(cursor).convertAndDispose()
        fileToClassNames.getOrPut(file, { mutableSetOf() }) += name
    }

    val result = mutableListOf<CValue<CXCursor>>()

    val translationUnits = classCursors.asSequence().mapNotNull { clang_Cursor_getTranslationUnit(it) }.distinct()

    for (translationUnit in translationUnits) {
        // Accessing the whole translation unit (TU) is overkill, but it is the simplest solution which is doable.
        // That's why we have to process all the classes in a single pass to avoid performance hit.
        //
        // Alternatively, we could e.g. use `clang_findReferencesInFile`. But that function does not seem to work for
        // this case, because for categories it returns `CXCursor_ObjCClassRef` (@interface >CLASS_REFERENCE<(CategoryName))
        // and there is no easy way to access category from there.
        visitChildren(translationUnit) { childCursor, _ ->
            if (childCursor.kind == CXCursorKind.CXCursor_ObjCCategoryDecl) {
                val categoryFile = getContainingFile(childCursor)
                val classNames = fileToClassNames[categoryFile]
                if (classNames != null) {
                    val categoryClassCursor = getObjCCategoryClassCursor(childCursor)
                    val categoryClassName = clang_getCursorDisplayName(categoryClassCursor).convertAndDispose()
                    if (categoryClassName in classNames) {
                        result += childCursor
                    }
                }
            }
            CXChildVisitResult.CXChildVisit_Continue
        }
    }

    return result
}

internal fun getObjCCategoryClassCursor(cursor: CValue<CXCursor>): CValue<CXCursor> {
    assert(cursor.kind == CXCursorKind.CXCursor_ObjCCategoryDecl)
    var classRef: CValue<CXCursor>? = null
    visitChildren(cursor) { child, _ ->
        if (child.kind == CXCursorKind.CXCursor_ObjCClassRef) {
            classRef = child
            CXChildVisitResult.CXChildVisit_Break
        } else {
            CXChildVisitResult.CXChildVisit_Continue
        }
    }

    return clang_getCursorReferenced(classRef!!).apply {
        assert(this.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl)
    }
}