/* Override preview iFrame with Authentication */
DataTableCellUI.prototype._previewCandidateTopic = function (candidate, elmt, preview, showActions) {
    var self = this;
    var id = candidate.id;
    var fakeMenu = $('<div></div>')
        .addClass("menu-container");
    fakeMenu
        .width(preview.width ? preview.width : 414)
        .addClass('data-table-topic-popup');
    if (showActions) {
        fakeMenu
            .html(DOM.loadHTML("core", "scripts/views/data-table/cell-recon-preview-popup-header.html"));
    }

    if (preview && preview.url) { // Service has a preview URL associated with it

        var authenticatorToken = '';
        var authenticatorHeader = '';
        $.ajax({
            url: 'command/openrefine-authenticator/get-authentication-token',
            type: 'GET',
            success: function (response) {
                if (response.code === 200) {
                    authenticatorToken = response.token;
                    authenticatorHeader = response.header;
                }
            },
            async: false
        });
        var url = encodeURI(preview.url.replace("{{id}}", id));
        // var iframe = $('<iframe></iframe>')
        //     .width(preview.width)
        //     .height(preview.height)
        //     .attr("src", url)
        //     .appendTo(fakeMenu);

        $.ajax({
            type: "GET",
            url: url,
            headers: {[authenticatorHeader]: authenticatorToken},
            success: function (data) {
                var iframe = $(`<div>${data.toString()}</div>`)
                    .width(preview.width)
                    .height(preview.height)
                    .appendTo(fakeMenu);

            }
        });

    } else {
        return; // no preview service available
    }

    MenuSystem.positionMenuLeftRight(fakeMenu, $(elmt));
    fakeMenu.appendTo(elmt);

    var dismissMenu = function () {
        fakeMenu.remove();
        fakeMenu.unbind();
    };

    if (showActions) {
        var elmts = DOM.bind(fakeMenu);

        elmts.matchButton.html($.i18n('core-views/match-cell'));
        elmts.matchSimilarButton.html($.i18n('core-views/match-identical'));
        elmts.cancelButton.html($.i18n('core-buttons/cancel'));

        elmts.matchButton.click(function () {
            self._doMatchTopicToOneCell(candidate);
            dismissMenu();
        });
        elmts.matchSimilarButton.click(function () {
            self._doMatchTopicToSimilarCells(candidate);
            dismissMenu();
        });
        elmts.cancelButton.click(function () {
            dismissMenu();
        });
    }
    return dismissMenu;
};

/**
 * Sets up a preview widget to appear when hovering the given DOM element.
 */
DataTableCellUI.prototype._previewOnHover = function (service, candidate, hoverElement, coreElement, showActions) {
    var self = this;
    var preview = null;
    if ((service) && (service.preview)) {
        preview = service.preview;
    }

    if (preview) {
        var dismissCallback = null;
        hoverElement.hover(function (evt) {
            if (!evt.metaKey && !evt.ctrlKey) {
                dismissCallback = self._previewCandidateTopic(candidate, coreElement, preview, showActions);
                evt.preventDefault();
            }
        }, function (evt) {
            if (dismissCallback !== null) {
                dismissCallback();
            }
        });
    }
};
