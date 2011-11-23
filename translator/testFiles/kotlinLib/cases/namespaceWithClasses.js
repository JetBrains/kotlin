{
  classes = function(){
    var A = Class.create({initialize:function(){
      this.$order = '';
      {
        this.set_order(this.get_order() + 'A');
      }
    }
    , set_order:function(tmp$0){
      this.$order = tmp$0;
    }
    , get_order:function(){
      return this.$order;
    }
    });
    var B = Class.create(A, {initialize:function(){
      this.super_init();
      {
        this.set_order(this.get_order() + 'B');
      }
    }
    });
    var C = Class.create(B, {initialize:function(){
      this.super_init();
      {
        this.set_order(this.get_order() + 'C');
      }
    }
    });
    return {A:A, B:B, C:C};
  }
  ();
  foo = Namespace.create(classes, {initialize:function(){
  }
  , box:function(){
    return (new foo.C).get_order() === 'ABC' && (new foo.B).get_order() === 'AB' && (new foo.A).get_order() === 'A';
  }
  });
}



function test() {
    return foo.box()
}
