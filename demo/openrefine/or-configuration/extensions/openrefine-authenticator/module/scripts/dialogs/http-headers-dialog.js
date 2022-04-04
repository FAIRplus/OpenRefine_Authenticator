/*
    Custom code from original OpenRefine project main/webapp/modules/core/scripts/dialogs/http-headers-dialog.js
*/

function HttpHeadersDialog(title, headers, onDone) {
    this._onDone = onDone;
    var self = this;

    var header = $('<div></div>').addClass("dialog-header").text(title).appendTo(frame);
    var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
    var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);
    var html = $(HttpHeadersDialog.generateWidgetHtml()).appendTo(body);
    this._elmts = DOM.bind(html);

    this._httpHeadersWidget = new HttpHeadersDialog.Widget(
        this._elmts,
        headers
    );
}

HttpHeadersDialog.generateWidgetHtml = function () {
    var html = DOM.loadHTML("core", "scripts/dialogs/http-headers-dialog.html");
    var httpheaderOptions = [];
    var token = ""
    var header = ""

    for (var headerLabel in theProject.httpHeaders) {
        if (theProject.httpHeaders.hasOwnProperty(headerLabel)) {
            var info = theProject.httpHeaders[headerLabel];

            if (headerLabel === 'authorization') {
                $.ajax({
                    url: 'command/openrefine-authenticator/get-authentication-token',
                    type: 'GET',
                    success: function (response) {
                        if (response.code === 200) {
                            token = response.token;
                            header = response.header;
                        }
                    },
                    async: false
                });
                info.defaultValue = token;
                headerLabel = header
            }

            httpheaderOptions.push('<label for="' +
                headerLabel +
                '">' +
                headerLabel +
                ': </label><input type="text" id="' +
                headerLabel +
                '" name="' +
                headerLabel +
                '" value="' +
                info.defaultValue +
                '" /></option><br />');
        }
    }

    return html.replace("$HTTP_HEADER_OPTIONS$", httpheaderOptions.join(""));
};

