function CredentialsDialog() {
}

CredentialsDialog.prototype = {
    init: function (callback) {
        var self = this;
        this.dialogElement = $(DOM.loadHTML("openrefine-authenticator", "dialogs/credentials.html"));

        /* Bind controls to actions */
        var controls = DOM.bind(this.dialogElement);
        controls.cancel.click(this.boundTo("hide"));
        controls.test.click(this.boundTo("test"));
        controls.save.click(this.boundTo("save"));

        /* Load service properties controls */
        $.getJSON('command/openrefine-authenticator/get-credentials', function (credentials) {
            var $credentials = $(controls.credentials);
            self.credentials = credentials;

            $credentials.append($('<label>', {
                    'for': 'auth_username',
                    text: 'Username: '
                }),
                $('<input>', {
                    id: 'auth_username', value: credentials['auth_username'],
                    change: function (event) {
                        credentials['auth_username'] = $(event.target).val();
                    },
                }));

            $credentials.append($('<label>', {
                    'for': 'auth_password',
                    text: 'Password: '
                }),
                $('<input>', {
                    id: 'auth_password', value: credentials['auth_password'],
                    type: 'password',
                    change: function (event) {
                        credentials['auth_password'] = $(event.target).val();
                    },
                }));

            if (callback)
                callback.apply(self);
        });
    },

    show: function () {
        this.init(function () {
            this.dialogLevel = DialogSystem.showDialog(this.dialogElement);
        });
    },

    hide: function () {
        DialogSystem.dismissUntil(this.dialogLevel - 1);
    },

    test: function () {
        $.ajax({
            url: 'command/openrefine-authenticator/test-credentials',
            type: "POST",
            data: JSON.stringify(this.credentials),
            success: function (response) {
                alert(response.message)
            },
        });
    },

    save: function () {
        $.ajax({
            url: 'command/openrefine-authenticator/save-credentials',
            type: "POST",
            data: JSON.stringify(this.credentials),
            success: this.boundTo("hide"),
        });
    },
};
