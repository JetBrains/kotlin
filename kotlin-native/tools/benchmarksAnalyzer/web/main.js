$(function() {
	var report = getUrlParameter('report');
	var compareToReport = getUrlParameter('compareTo');
    var hostname = window.location.href.substring(0, window.location.href.lastIndexOf('/'));
    if (report === undefined) {
        alert("Need to provide report parameter");
        throw new Error("Need to provide report parameter");
    }
    if (compareToReport === undefined) {
        benchmarksAnalyzer.main_kand9s$([report, "-r", "html", "-o", "html"]);
    } else {
	   benchmarksAnalyzer.main_kand9s$([report,compareToReport, "-r", "html", "-o", "html"]);
    }
});

var getUrlParameter = function getUrlParameter(sParam) {
    var sPageURL = window.location.search.substring(1),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
        }
    }
};
