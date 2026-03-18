import {
    acceptCovariant,
    acceptContravariant,
    acceptInvariant,
} from "./index.mjs"

acceptCovariant({ value: "hello" })
acceptContravariant({ consume(_: any) {} })
acceptInvariant({ value: 42 })