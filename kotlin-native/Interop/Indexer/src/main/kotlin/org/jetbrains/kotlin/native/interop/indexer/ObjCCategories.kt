/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.CValue

class ObjCCategoriesIndex {
    private class Categories(val classNameToCategoriesInSameFile: Map<String, List<CValue<CXCursor>>>)

    private fun findAllCategories(translationUnit: CXTranslationUnit): Categories {
        val classNameToCategoriesInSameFile = mutableMapOf<String, MutableList<CValue<CXCursor>>>()

        // Accessing the whole translation unit (TU) is overkill, but it is the simplest solution which is doable.
        // That's why we have to cache the result to avoid performance hit.
        //
        // Alternatively, we could e.g. use `clang_findReferencesInFile`. But that function does not seem to work for
        // this case, because for categories it returns `CXCursor_ObjCClassRef` (@interface >CLASS_REFERENCE<(CategoryName))

        visitChildren(translationUnit) { childCursor, _ ->
            if (childCursor.kind == CXCursorKind.CXCursor_ObjCCategoryDecl) {
                val classCursor = getObjCCategoryClassCursor(childCursor)
                val className = clang_getCursorDisplayName(classCursor).convertAndDispose()
                val classFile = getContainingFile(classCursor)

                val categoryFile = getContainingFile(childCursor)
                if (clang_File_isEqual(categoryFile, classFile) != 0) {
                    classNameToCategoriesInSameFile.getOrPut(className, { mutableListOf() }) += childCursor
                }
            }
            CXChildVisitResult.CXChildVisit_Continue
        }

        return Categories(classNameToCategoriesInSameFile = classNameToCategoriesInSameFile)
    }

    private val translationUnitToCategories = mutableMapOf<CXTranslationUnit, Categories>()

    /**
     * Find all categories for a class that is pointed by [classCursor] in the same file.
     *
     * NB: Current implementation is rather slow as it walks the whole translation unit.
     * But only for the first time - after that, it caches the result.
     */
    fun findCategoriesInTheSameFile(classCursor: CValue<CXCursor>, className: String): List<CValue<CXCursor>> {
        check(classCursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl) { classCursor.kind }

        val translationUnit = clang_Cursor_getTranslationUnit(classCursor) ?: return emptyList()

        return translationUnitToCategories.getOrPut(translationUnit) {
            findAllCategories(translationUnit)
        }.classNameToCategoriesInSameFile[className].orEmpty()
    }
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
