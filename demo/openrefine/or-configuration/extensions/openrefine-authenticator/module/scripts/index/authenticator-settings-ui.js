Refine.SetAuthenticarUI = function (elmt) {
    var self = this;
    elmt.html(DOM.loadHTML("openrefine-authenticator", "scripts/index/authenticator-settings-ui.html"));

    this._elmt = elmt;
    this._elmts = DOM.bind(elmt);

    this._elmts.authenticator_label.text("Authenticator configuration:");

    var controls = this._elmts;


    /* Load authentication configuration */
    $.ajax({
        url: 'command/openrefine-authenticator/get-authentication',
        type: 'GET',
        async: false,
        success: function (configuration) {
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
        }
    });

    /* Load authentication credentials */
    $.ajax({
        url: 'command/openrefine-authenticator/get-credentials',
        type: 'GET',
        async: false,
        success: function (credentials) {
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
        }
    });

    this._elmts.test.bind('click', function () {
        $.ajax({
            url: 'command/openrefine-authenticator/test-credentials',
            type: "POST",
            data: JSON.stringify(self.credentials),
            success: function (response) {
                alert(response.message)
            },
        });
    });

    this._elmts.save.bind('click', function () {
        $.ajax({
            url: 'command/openrefine-authenticator/save-credentials',
            type: "POST",
            async: false,
            data: JSON.stringify(self.credentials)
        });
        $.ajax({
            url: 'command/openrefine-authenticator/save-authentication',
            type: "PUT",
            data: JSON.stringify(self.configuration),
            success: alert("Saved"),
        });
    });
};

Refine.SetAuthenticarUI.prototype.resize = function () {
};

Refine.actionAreas.push({
    id: "authenticator-settings",
    label: "Authenticator Settings",
    uiClass: Refine.SetAuthenticarUI
});
