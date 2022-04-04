function AboutDialog() {
}

AboutDialog.prototype = {
    init: function () {
        this.dialogElement = $(DOM.loadHTML("openrefine-authenticator", "dialogs/about.html"));
        var controls = DOM.bind(this.dialogElement);
        controls.close.click(this.boundTo("hide"));
    },

    show: function () {
        this.init();
        this.dialogLevel = DialogSystem.showDialog(this.dialogElement);
    },

    hide: function () {
        DialogSystem.dismissUntil(this.dialogLevel - 1);
    },
};
