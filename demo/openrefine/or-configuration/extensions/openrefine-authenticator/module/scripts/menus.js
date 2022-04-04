/* Add menu to extension bar */
ExtensionBar.addExtensionMenu({
    id: "openrefine-authenticator",
    label: "Authenticator configuration",
    submenu: [
        {
            id: "openrefine-authenticator/configuration",
            label: "Configure...",
            click: dialogHandler(ConfigurationDialog),
        },
        {
            id: "openrefine-authenticator/credentials",
            label: "Credentials...",
            click: dialogHandler(CredentialsDialog),
        },
        { /* separator */},
        {
            id: "openrefine-authenticator/about",
            label: "About...",
            click: dialogHandler(AboutDialog),
        },
    ]
});

function dialogHandler(dialogConstructor) {
    var dialogArguments = Array.prototype.slice.call(arguments, 1);

    function Dialog() {
        return dialogConstructor.apply(this, dialogArguments);
    }

    Dialog.prototype = dialogConstructor.prototype;
    return function () {
        new Dialog().show();
    };
}


// Bind a method to an object and cache it
// via: http://webreflection.blogspot.be/2012/11/my-name-is-bound-method-bound.html
Object.defineProperty(Object.prototype, "boundTo", {
    value: function (methodName) {
        var boundName = "__bound__" + methodName;
        return this[boundName] || (this[boundName] = this[methodName].bind(this));
    },
});