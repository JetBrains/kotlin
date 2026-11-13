(module
  ;; Field-evolution experiment, world V1 — the world the -Xwasm-hot-reload
  ;; compiler now emits (Stage 1, validated 2026-11-11):
  ;;
  ;;   $Cell  the class struct: stable FOREVER (vtable/itable/rtti elided —
  ;;          they don't matter here). Single mutable payload field, typed
  ;;          with the ROOT payload type so every payload version and every
  ;;          subclass payload is storable (mutable fields are invariant in
  ;;          wasm GC subtyping, so the field type must be hierarchy-wide).
  ;;   $P0    root payload ("payload<Any>" analog; real one holds _hashCode)
  ;;   $P1    payload version 1 of class Pt: {a}
  ;;
  ;; All three in ONE rec group (Cell references P0; P1 references P0).
  ;; Redefined worlds must re-declare this group BYTE-IDENTICALLY so it
  ;; canonicalizes to the same runtime types (the driver8 result), and put
  ;; new payload versions in their OWN groups.
  (rec
    (type $P0 (sub (struct)))
    (type $Cell (sub (struct (field (mut (ref null $P0))))))
    (type $P1 (sub $P0 (struct (field (mut i32))))) ;; a
  )

  (func $make (export "makeV1") (param i32) (result (ref $Cell))
    (struct.new $P1 (local.get 0))
    (struct.new $Cell))

  ;; Old code's field access: cell payload extract + version cast + read.
  ;; The cast IS the version check — on a migrated-to-deletion instance it
  ;; traps (the invalidation trampoline); on a migrated-to-addition instance
  ;; it succeeds because payload v2 is a SUBTYPE of v1 (surviving fields keep
  ;; their flat indices by construction).
  (func $getA (export "getA_v1") (param (ref null $Cell)) (result i32)
    (ref.cast (ref $P1) (struct.get $Cell 0 (local.get 0)))
    (struct.get $P1 0))

  ;; Identity is the CELL — never the payload.
  (func $isV1 (export "isV1") (param anyref) (result i32)
    (ref.test (ref null $Cell) (local.get 0)))
)
