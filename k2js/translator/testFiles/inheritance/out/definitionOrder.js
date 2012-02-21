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
      this.set_order(this.get_order() + 'D');
    }
  }
  });
  var tmp$3 = Kotlin.Class.create(tmp$1, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'C');
    }
  }
  });
  var tmp$4 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
    {
      this.set_order(this.get_order() + 'E');
    }
  }
  });
  return {B:tmp$1, D:tmp$2, C:tmp$3, E:tmp$4, A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    return (new foo.C).get_order() == 'ABC' && (new foo.D).get_order() == 'ABD' && (new foo.E).get_order() == 'AE';
  }
}
}, {C:classes.C, D:classes.D, E:classes.E, B:classes.B, A:classes.A});
foo.initialize();
