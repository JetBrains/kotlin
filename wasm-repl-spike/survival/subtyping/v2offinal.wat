(module
  (rec
    (type $Pt (sub final (struct (field (mut i32)) (field (mut i32)))))
  )
  (rec
    (type $Pt2 (sub $Pt (struct (field (mut i32)) (field (mut i32)) (field (mut i32)))))
  )
  (func $make (export "makeV2") (result (ref $Pt2))
    i32.const 33
    i32.const 44
    i32.const 55
    struct.new $Pt2)
)
