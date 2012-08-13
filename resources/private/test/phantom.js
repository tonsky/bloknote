if (phantom.args.length != 1) {
    console.log('Expected a target URL parameter.');
    phantom.exit(1);
}

var page = require('webpage').create();
var url = phantom.args[0];

page.onConsoleMessage = function (message) {
    console.log("=> " + message);
};

console.log("Loading URL: " + url);

page.open(url, function (status) {
    if (status != "success") {
        console.log('Failed to open ' + url);
        phantom.exit(1);
    }

    console.log("Running tests...");

    var result = page.evaluate(function() {
        return bloknote.test_editor.run();
    });

    // NOTE: PhantomJS 1.4.0 has a bug that prevents the exit codes
    //        below from being returned properly. :(
    //
    // http://code.google.com/p/phantomjs/issues/detail?id=294

    if (result != 0) {
        console.log("*** Tests failed! ***");
        phantom.exit(1);
    }

    console.log("Tests succeeded.");
    phantom.exit(0);
});