function ConfigurationDialog() {
}

ConfigurationDialog.prototype = {
    init: function (callback) {
        var self = this;
        this.dialogElement = $(DOM.loadHTML("openrefine-authenticator", "dialogs/configuration.html"));

        /* Bind controls to actions */
        var controls = DOM.bind(this.dialogElement);
        controls.cancel.click(this.boundTo("hide"));
        controls.update.click(this.boundTo("update"));

        /* Load service properties controls */
        $.getJSON('command/openrefine-authenticator/get-authentication', function (configuration) {
            var $configuration = $(controls.configuration);
            self.configuration = configuration;

            $configuration.append($('<label>', {'for': 'auth_type_options', text: 'Authentication Type: '}));
            var select_auth_type_options = $('<select>', {
                id: 'auth_type_options',
                change: function (event) {
                    configuration['auth_type'] = $(event.target).val();
                },
            });
            for (const i in configuration['auth_type_options']) {
                const auth_type_option = configuration['auth_type_options'][i]
                select_auth_type_options.append($("<option>").attr('value', auth_type_option).text(auth_type_option));
            }
            select_auth_type_options.val(configuration['auth_type'])
            $configuration.append(select_auth_type_options)


            $configuration.append($('<label>', {
                'for': 'auth_token_type_options',
                text: 'Authentication Token Type: '
            }));
            var auth_token_type_options = $('<select>', {
                id: 'auth_token_type_options',
                change: function (event) {
                    configuration['auth_token_type'] = $(event.target).val();
                },
            });
            for (const i in configuration['auth_token_type_options']) {
                const auth_type_option = configuration['auth_token_type_options'][i]
                auth_token_type_options.append($("<option>").attr('value', auth_type_option).text(auth_type_option));
            }
            auth_token_type_options.val(configuration['auth_token_type'])
            $configuration.append(auth_token_type_options)


            $configuration.append($('<label>', {
                'for': 'auth_header_options',
                text: 'Authentication Header Key: '
            }));
            var auth_header_options = $('<select>', {
                id: 'auth_header_options',
                change: function (event) {
                    configuration['auth_header'] = $(event.target).val();
                },
            });
            for (const i in configuration['auth_header_options']) {
                const auth_type_option = configuration['auth_header_options'][i]
                auth_header_options.append($("<option>").attr('value', auth_type_option).text(auth_type_option));
            }
            auth_header_options.val(configuration['auth_header'])
            $configuration.append(auth_header_options)

            $configuration.append($('<label>', {'for': 'auth_endpoint', text: 'Authentication Endpoint: '}),
                $('<input>', {
                    id: 'auth_endpoint', value: configuration['auth_endpoint'],
                    change: function (event) {
                        configuration['auth_endpoint'] = $(event.target).val();
                    },
                }));

            $configuration.append($('<label>', {
                    'for': 'auth_username_property',
                    text: 'Authentication Username Property: '
                }),
                $('<input>', {
                    id: 'auth_username_property', value: configuration['auth_username_property'],
                    change: function (event) {
                        configuration['auth_username_property'] = $(event.target).val();
                    },
                }));

            $configuration.append($('<label>', {
                    'for': 'auth_password_property',
                    text: 'Authentication Password Property: '
                }),
                $('<input>', {
                    id: 'auth_password_property', value: configuration['auth_password_property'],
                    change: function (event) {
                        configuration['auth_password_property'] = $(event.target).val();
                    },
                }));

            $configuration.append($('<label>', {'for': 'auth_token_property', text: 'Authentication Token Property: '}),
                $('<input>', {
                    id: 'auth_token_property', value: configuration['auth_token_property'],
                    change: function (event) {
                        configuration['auth_token_property'] = $(event.target).val();
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

    update: function () {
        $.ajax({
            url: 'command/openrefine-authenticator/save-authentication',
            type: "PUT",
            data: JSON.stringify(this.configuration),
            success: this.boundTo("hide"),
        });
    },
};
