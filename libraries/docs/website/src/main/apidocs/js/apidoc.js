// This execute once the document loads.
$(function(){
	$("div.source-detail").hide(); 
	$("div.doc-member").hover(
      function() { $(this).children("div.source-detail").show(); },
      function() { $(this).children("div.source-detail").hide(); }
 	);

    var searchUrl = $( "#searchBox" ).attr("data-url");
    $.ajax({
        url: searchUrl,
        dataType: "xml",
        success: function( xmlResponse ) {
            var data = $( "search", xmlResponse ).map(function() {
                return {
                    value: $( "kind", this ).text() + " " + $( "name", this ).text(),
                    id: $( "href", this ).text()
                };
            }).get();
            $( "#searchBox" ).autocomplete({
                source: data,
                minLength: 0,
                select: function( event, ui ) {
                    if (ui.item) {
                        var url = ui.item.id
                        if (url) {
                            // lets append it to the search url
                            var idx = searchUrl.lastIndexOf("/");
                            var dir = ""
                            if (idx > 0) {
                                dir = searchUrl.slice(0, idx) + "/";
                            }
                            var fullUrl = dir + url;
                            window.location.href = fullUrl
                        }
                    }
                }
            });
        }
    });
});