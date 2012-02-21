var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$order = '';
    {
      this.set_order(this.get_order() + 'A');
    }
  }
  , get_order:function(){
    return this.$order;
  }
  , set_order:function(tmp$0){
    this.$order = tmp$0;
  }
  });
  var tmp$1 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'B');
    }
  }
  });
  var tmp$2 = Kotlin.Class.create(tmp$1, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'C');
    }
  }
  });
  return {B:tmp$1, C:tmp$2, A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.C).get_order() == 'ABC' && (new foo.B).get_order() == 'AB' && (new foo.A).get_order() == 'A';
  }
}
}, {A:classes.A, B:classes.B, C:classes.C});
foo.initialize();
