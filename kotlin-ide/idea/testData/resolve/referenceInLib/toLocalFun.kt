import inlibrary.test.*

val tl = <caret>topLevel()

// CONTEXT: return <ref-caret>local()
// WITH_LIBRARY: /resolve/referenceInLib/inLibrarySource

// REF: (in inlibrary.test.topLevel).local()