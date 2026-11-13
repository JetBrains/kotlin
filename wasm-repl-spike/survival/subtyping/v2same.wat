(module
  ;; v1 AND v2 in the SAME rec group — if rec-group identity is whole-group,
  ;; this v1 may NOT canonicalize to module v1's $Pt, breaking the chain.
  (rec
    (type $Pt (sub (struct (field (mut i32)) (field (mut i32)))))
    (type $Pt2 (sub $Pt (struct (field (mut i32)) (field (mut i32)) (field (mut i32)))))
  )
  (func $make (export "makeV2") (result (ref $Pt2)
    i32.const 33
    i32.const 44
    i32.const 55
    struct.new $Pt2)
  )
  (func $take (export "takeV2") (param (ref null $Pt2)) (result i32)
    local.get 0
    struct.get $Pt2 2)
)
