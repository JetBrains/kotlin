# binary-compatibility-extended

This project is a re-implementation of https://github.com/Kotlin/binary-compatibility-validator,
based on https://github.com/adamko-dev/kotlin-binary-compatibility-validator-mu.

binary-compatibility-extended supports generating multiple ABI dumps inside a single project,
which is useful for separately tracking 'public' API and 'internal' API. 
