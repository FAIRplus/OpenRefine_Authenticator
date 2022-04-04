/*
    Custom code from original OpenRefine project main/webapp/modules/core/scripts/dialogs/extend-data-preview-dialog.js
 */

ExtendReconciledDataPreviewDialog.prototype._update = function () {
    this._elmts.previewContainer.empty().html(
        '<div bind="progressPanel" class="extend-data-preview-progress"><img src="images/large-spinner.gif" /></div>');

    var self = this;
    var params = {
        project: theProject.id,
        columnName: this._column.name
    };

    if (this._extension.properties.length === 0) {
        // if the column selection is empty, reset the view
        this._elmts.previewContainer.empty();
    } else {
        // otherwise, refresh the preview
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
        Refine.postCSRF(
            "command/openrefine-authenticator/preview-extend-data?" + $.param(params),
            {
                rowIndices: JSON.stringify(this._rowIndices),
                extension: JSON.stringify(this._extension),
                authenticatorHeader: authenticatorHeader,
                authenticatorToken: authenticatorToken
            },
            function (data) {
                self._renderPreview(data);
            },
            "json",
            function (data) {
                alert($.i18n('core-views/internal-err'));
            });
    }
};

