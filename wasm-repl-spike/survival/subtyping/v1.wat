(module
  ;; v1 in its OWN rec group, declared OPEN (sub) so it can have subtypes.
  (rec
    (type $Pt (sub (struct (field (mut i32)) (field (mut i32)))))
  )
  (func $make (export "makeV1") (result (ref $Pt))
    i32.const 11
    i32.const 22
    struct.new $Pt)
  (func $take (export "takeV1") (param (ref null $Pt)) (result i32)
    local.get 0
    struct.get $Pt 0)
  (func $isV1 (export "isV1") (param anyref) (result i32)
    local.get 0
    ref.test (ref null $Pt))
)
