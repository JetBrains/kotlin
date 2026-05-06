class Child extends Parent {
  constructor() {
    return new.target.new_Child_enp6fv_k$();
  }
  static new_Child_enp6fv_k$($box) {
    var $this = this.new_Parent_wmhgih_k$($box);
    $this.foo_2 = null;
    return $this;
  }
  get_foo_18j5pf_k$() {
    return this.foo_2;
  }
  get_foo_sfydlo_k$() {
    return this.get_foo_18j5pf_k$();
  }
}
