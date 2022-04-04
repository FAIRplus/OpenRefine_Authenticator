/* Replace submenu core/add-column-by-reconciliation */
DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
    // id: core/add-column-by-reconciliation
    MenuSystem.find(menu, ["core/edit-column"], 1)[5].click = function () {
        var columnIndex = Refine.columnNameToColumnIndex(column.name);
        var o = DataTableView.sampleVisibleRows(column);
        new ExtendReconciledDataPreviewDialog(
            column,
            columnIndex,
            o.rowIndices,
            function (extension, endpoint, identifierSpace, schemaSpace) {

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
                Refine.postProcess(
                    "openrefine-authenticator",
                    "extend-data",
                    {
                        baseColumnName: column.name,
                        endpoint: endpoint,
                        identifierSpace: identifierSpace,
                        schemaSpace: schemaSpace,
                        columnInsertIndex: columnIndex + 1
                    },
                    {
                        extension: JSON.stringify(extension),
                        authenticatorHeader: authenticatorHeader,
                        authenticatorToken: authenticatorToken
                    },
                    {rowsChanged: true, modelsChanged: true}
                );
            }
        );
    };

});