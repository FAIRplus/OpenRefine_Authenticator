/* Replace submenu core/add-column-by-fetching-urls */
DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
    var columnIndex = Refine.columnNameToColumnIndex(column.name);
    // id: core/add-column-by-fetching-urls
    MenuSystem.find(menu, ["core/edit-column"], 1)[4].click = function () {
        var frame = $(
            DOM.loadHTML("core", "scripts/views/data-table/add-column-by-fetching-urls-dialog.html")
                .replace("$EXPRESSION_PREVIEW_WIDGET$", ExpressionPreviewDialog.generateWidgetHtml())
                .replace("$HTTP_HEADERS_WIDGET$", HttpHeadersDialog.generateWidgetHtml())
        );

        var elmts = DOM.bind(frame);
        elmts.dialogHeader.text($.i18n('core-views/add-col-fetch') + " " + column.name);

        elmts.or_views_newCol.text($.i18n('core-views/new-col-name'));
        elmts.or_views_throttle.text($.i18n('core-views/throttle-delay'));
        elmts.or_views_milli.text($.i18n('core-views/milli'));
        elmts.or_views_onErr.text($.i18n('core-views/on-error'));
        elmts.or_views_setBlank.text($.i18n('core-views/set-blank'));
        elmts.or_views_storeErr.text($.i18n('core-views/store-err'));
        elmts.or_views_cacheResponses.text($.i18n('core-views/cache-responses'));
        elmts.or_views_httpHeaders.text($.i18n('core-views/http-headers'));
        elmts.or_views_httpHeadersShowHide.text($.i18n('core-views/show'));
        elmts.or_views_httpHeadersShowHide.click(function () {
            $(".set-httpheaders-container").toggle("slow", function () {
                if ($(this).is(':visible')) {
                    elmts.or_views_httpHeadersShowHide.text($.i18n('core-views/hide'));
                } else {
                    elmts.or_views_httpHeadersShowHide.text($.i18n('core-views/show'));
                }
            });
        });
        elmts.or_views_urlFetch.text($.i18n('core-views/url-fetch'));
        elmts.okButton.html($.i18n('core-buttons/ok'));
        elmts.cancelButton.text($.i18n('core-buttons/cancel'));

        var level = DialogSystem.showDialog(frame);
        var dismiss = function () {
            DialogSystem.dismissUntil(level - 1);
        };

        var o = DataTableView.sampleVisibleRows(column);
        var previewWidget = new ExpressionPreviewDialog.Widget(
            elmts,
            column.cellIndex,
            o.rowIndices,
            o.values,
            null
        );


        elmts.cancelButton.click(dismiss);
        elmts.okButton.click(function () {
            var columnName = $.trim(elmts.columnNameInput[0].value);
            if (!columnName.length) {
                alert($.i18n('core-views/warning-col-name'));
                return;
            }

            Refine.postCoreProcess(
                "add-column-by-fetching-urls",
                {
                    baseColumnName: column.name,
                    urlExpression: previewWidget.getExpression(true),
                    newColumnName: columnName,
                    columnInsertIndex: columnIndex + 1,
                    delay: elmts.throttleDelayInput[0].value,
                    onError: $('input[name="dialog-onerror-choice"]:checked')[0].value,
                    cacheResponses: $('input[name="dialog-cache-responses"]')[0].checked,
                    httpHeaders: JSON.stringify(elmts.setHttpHeadersContainer.find("input").serializeArray())
                },
                null,
                {modelsChanged: true}
            );
            dismiss();
        });
    };

});