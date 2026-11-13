(module
  ;; World V2 — class Pt REDEFINED with an ADDED field b (payload version 2).
  ;; Rec group 1 re-declares the old world byte-identically; rec group 2 is
  ;; the new payload version, a declared SUBTYPE of v1 with the field appended
  ;; (the driver8 placement rule: separate rec group, or canonicalization
  ;; breaks and existing instances stop unifying).
  (rec
    (type $P0 (sub (struct)))
    (type $Cell (sub (struct (field (mut (ref null $P0))))))
    (type $P1 (sub $P0 (struct (field (mut i32))))) ;; a
  )
  (rec
    (type $P2 (sub $P1 (struct (field (mut i32)) (field (mut i32))))) ;; a, b
  )

  (func $make (export "makeV2") (param i32 i32) (result (ref $Cell))
    (struct.new $P2 (local.get 0) (local.get 1))
    (struct.new $Cell))

  ;; The compiler-generated lazy migration (CL update-instance-for-redefined-
  ;; class default): copy surviving fields through the OLD version's accessors,
  ;; fill new fields from their initforms (here: 42), swap the cell's payload.
  ;; Runs at most once per instance — after the swap the version test passes.
  (func $migrate (export "migrateAdd") (param (ref $Cell)) (result i32)
    (local $old (ref null $P0))
    (local.set $old (struct.get $Cell 0 (local.get 0)))
    (struct.set $Cell 0 (local.get 0)
      (struct.new $P2
        (struct.get $P1 0 (ref.cast (ref $P1) (local.get $old)))
        i32.const 42))
    (struct.get $P2 1 (ref.cast (ref $P2) (struct.get $Cell 0 (local.get 0)))))

  ;; New code's field access, with the version check made explicit:
  ;; test -> migrate on miss -> read. (Stage 1's ref.cast is exactly this
  ;; check with no migrate arm yet; the compiler lowering would insert a
  ;; catchable ObsoleteInstanceException instead of a raw trap for
  ;; *deleted* fields.)
  (func $getB (export "getB_v2") (param (ref null $Cell)) (result i32)
    (if (result i32) (ref.test (ref $P2) (struct.get $Cell 0 (local.get 0)))
      (then (struct.get $P2 1 (ref.cast (ref $P2) (struct.get $Cell 0 (local.get 0)))))
      (else (call $migrate (ref.as_non_null (local.get 0))))))

  ;; New code reading a SURVIVING field: the v2 payload's prefix is v1, so
  ;; casting to the declaring payload works for both fresh v2 payloads and
  ;; un-migrated v1 payloads alike (v1 is not a v2 — but `a` lives in the
  ;; shared prefix; cast to $P1, read flat 0).
  (func $getA (export "getA_v2") (param (ref null $Cell)) (result i32)
    (struct.get $P1 0 (ref.cast (ref $P1) (struct.get $Cell 0 (local.get 0)))))

  (func $isV2 (export "isV2") (param anyref) (result i32)
    (ref.test (ref null $Cell) (local.get 0)))
)
