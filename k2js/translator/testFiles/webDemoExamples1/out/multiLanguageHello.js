var classes = function(){
  return {};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, main:function(args){
  {
    var tmp$2;
    var tmp$1;
    var tmp$0;
    if (args.length == 0)
      tmp$0 = 'EN';
    else 
      tmp$0 = args[0];
    var language = tmp$0;
    for (tmp$1 = 0; tmp$1 < 4; ++tmp$1) {
      if (tmp$1 == 0)
        if (language == 'EN') {
          tmp$2 = 'Hello!';
          break;
        }
      if (tmp$1 == 1)
        if (language == 'FR') {
          tmp$2 = 'Salut!';
          break;
        }
      if (tmp$1 == 2)
        if (language == 'IT') {
          tmp$2 = 'Ciao!';
          break;
        }
      if (tmp$1 == 3)
        tmp$2 = "Sorry, I can't greet you in " + language + ' yet';
    }
    Kotlin.println(tmp$2);
  }
}
}, {});
Anonymous.initialize();
