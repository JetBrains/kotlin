#mimalloc

mimalloc is a general purpose allocator with excellent performance characteristics.
Initially developed by Daan Leijen for the run-time systems of the Koka and Lean languages.

Source code: https://github.com/microsoft/mimalloc
Used version: 1.6.7 (https://github.com/microsoft/mimalloc/releases/tag/v1.6.7)

The constant KONAN_MI_MALLOC is used to integrate mimalloc code in K/N runtime.
All changes that are done should be under directives `#if defined(KONAN_MI_MALLOC)`

To add code, do:

	#if defined(KONAN_MI_MALLOC)
    <new code>
    #endif // KONAN_MI_MALLOC
    
To delete code, do:

    #if !defined(KONAN_MI_MALLOC)
    <code to delete>
    #endif // KONAN_MI_MALLOC

To modify code, do:

    #if !defined(KONAN_MI_MALLOC)
    <current code>
    #else // KONAN_MI_MALLOC
    <modified code>
    #endif // KONAN_MI_MALLOC

or

    #if defined(KONAN_MI_MALLOC)
    <modified code>
    #else // KONAN_MI_MALLOC
    <current code>
    #endif // KONAN_MI_MALLOC

