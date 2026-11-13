(module
  ;; World V3 — class Pt REDEFINED with field a DELETED (payload version 2').
  ;; The new payload is a SIBLING of v1, not a child: wasm GC subtyping can
  ;; extend field lists but never shrink them, so a deletion cannot keep the
  ;; old payload in the ancestry. Consequences, all engine-enforced:
  ;;
  ;;   - the cell still accepts the new payload (both are $P0 subtypes —
  ;;     the payload field's invariant type is the ROOT payload);
  ;;   - OLD code touching the deleted field on a migrated instance fails
  ;;     its version cast — the trap IS the invalidation trampoline. The
  ;;     fields are literally gone; "keeping them but inaccessible" is what
  ;;     the version cast gives us, with no shadow copy at all;
  ;;   - migration reads the doomed fields out first (CL's
  ;;     update-instance-for-redefined-class hands them to the update hook
  ;;     before dropping them).
  (rec
    (type $P0 (sub (struct)))
    (type $Cell (sub (struct (field (mut (ref null $P0))))))
    (type $P1 (sub $P0 (struct (field (mut i32))))) ;; a
  )
  (rec
    (type $P2 (sub $P0 (struct))) ;; {} — a removed
  )

  (func $make (export "makeDel") (result (ref $Cell))
    (struct.new $P2)
    (struct.new $Cell))

  ;; Migration with deletion: read the doomed field out through the old
  ;; version's accessor, swap in the new (smaller) payload, report what died.
  (func $migrate (export "migrateDel") (param (ref $Cell)) (result i32)
    (local $old (ref null $P0))
    (local.set $old (struct.get $Cell 0 (local.get 0)))
    (struct.set $Cell 0 (local.get 0) (struct.new $P2))
    (struct.get $P1 0 (ref.cast (ref $P1) (local.get $old))))

  ;; New code on the new world: any instance, once migrated, type-checks.
  (func $noop (export "noopDel") (param (ref null $Cell)) (result i32)
    (if (result i32) (ref.test (ref $P2) (struct.get $Cell 0 (local.get 0)))
      (then i32.const 1)
      (else (drop (call $migrate (ref.as_non_null (local.get 0)))) i32.const 1)))
)
