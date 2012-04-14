// This execute once the document loads.
$(function(){
	$("div.source-detail").hide(); 
	$("div.doc-member").hover(
      function() { $(this).children("div.source-detail").show(); },
      function() { $(this).children("div.source-detail").hide(); }
 	);
});