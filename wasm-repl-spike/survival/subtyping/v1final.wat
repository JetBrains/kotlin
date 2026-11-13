(module
  (rec
    (type $Pt (sub final (struct (field (mut i32)) (field (mut i32)))))
  )
  (func $make (export "makeV1") (result (ref $Pt))
    i32.const 11
    i32.const 22
    struct.new $Pt)
)
