(module
  ;; rec group 1: v1 re-declared IDENTICALLY (content-addressed -> should
  ;; canonicalize to the same type as module v1's).
  (rec
    (type $Pt (sub (struct (field (mut i32)) (field (mut i32)))))
  )
  ;; rec group 2 (SEPARATE): v2 as a declared subtype of v1, field appended.
  (rec
    (type $Pt2 (sub $Pt (struct (field (mut i32)) (field (mut i32)) (field (mut i32)))))
  )
  (func $make (export "makeV2") (result (ref $Pt2))
    i32.const 33
    i32.const 44
    i32.const 55
    struct.new $Pt2)
  (func $take (export "takeV2") (param (ref null $Pt2)) (result i32)
    local.get 0
    struct.get $Pt2 2)
  (func $isV2 (export "isV2") (param anyref) (result i32)
    local.get 0
    ref.test (ref null $Pt2))
)
