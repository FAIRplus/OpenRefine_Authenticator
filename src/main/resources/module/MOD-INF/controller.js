var logger = Packages.org.slf4j.LoggerFactory.getLogger("openrefine-authenticator"),
    refinepro = Packages.com.refinepro,
    refineServlet = Packages.com.google.refine.RefineServlet;

/* Initialize the extension. */
function init() {
    logger.info("Initializing Authenticator manager");
    const initializer = new refinepro.app.InitializationCommand();
    refineServlet.registerCommand(module, "initialize", initializer);
    const applicationContext = new refinepro.app.ApplicationContext();
    initializer.initApplicationContext(applicationContext);

    refineServlet.registerCommand(module, "get-authentication", new refinepro.commands.GetAuthenticationCommand(applicationContext));
    refineServlet.registerCommand(module, "get-authentication-token", new refinepro.commands.GetAuthenticationTokenCommand(applicationContext));
    refineServlet.registerCommand(module, "get-credentials", new refinepro.commands.GetCredentialsCommand(applicationContext));
    refineServlet.registerCommand(module, "test-credentials", new refinepro.commands.TestCredentialsCommand(applicationContext));
    refineServlet.registerCommand(module, "save-authentication", new refinepro.commands.SaveAuthenticationCommand(applicationContext));
    refineServlet.registerCommand(module, "save-credentials", new refinepro.commands.SaveCredentialsCommand(applicationContext));

    refineServlet.registerCommand(module, "reconcile", new refinepro.commands.recon.ReconcileCommand());
    refineServlet.registerCommand(module, "guess-types-of-column", new refinepro.commands.recon.GuessTypesOfColumnCommand());
    refineServlet.registerCommand(module, "preview-extend-data", new refinepro.commands.recon.PreviewExtendDataCommand());
    refineServlet.registerCommand(module, "extend-data", new refinepro.commands.recon.ExtendDataCommand());

    var OR = Packages.com.google.refine.operations.OperationRegistry;
    OR.registerOperation(module, "extend-reconciled-data", refinepro.operations.recon.ExtendDataOperation);

    // Register importer and exporter
    var IM = Packages.com.google.refine.importing.ImportingManager;
    IM.registerController(
        module,
        "default-importing-controller",
        new Packages.com.refinepro.importing.DefaultImportingController()
    );

    var RC = Packages.com.google.refine.model.recon.ReconConfig;
    RC.registerReconConfig(module, "standard-service-authenticator", refinepro.model.recon.StandardReconConfig);

    var resourceManager = Packages.com.google.refine.ClientSideResourceManager;

    resourceManager.addPaths(
        "index/scripts",
        module,
        [
            "scripts/index/default-importing-controller/controller.js",
            "scripts/index/authenticator-settings-ui.js",
        ]
    );

    resourceManager.addPaths(
        "index/styles",
        module,
        [
            "scripts/index/authenticator-settings-ui.less",
        ]
    );
    resourceManager.addPaths(
        "project/scripts",
        module, [
            "dialogs/about.js",
            "dialogs/configuration.js",
            "dialogs/credentials.js",
            "scripts/dialogs/http-headers-dialog.js",
            "scripts/dialogs/extend-data-preview-dialog.js",
            "scripts/reconciliation/standard-service-panel.js",
            "scripts/reconciliation/recon-manager.js",
            "scripts/menus.js",
            "scripts/fetch-url.js",
            "scripts/extend-data.js",
            "scripts/cell-ui.js",
        ]
    );
    resourceManager.addPaths(
        "project/styles",
        module, [
            "dialogs/about.less",
            "dialogs/configuration.less",
            "dialogs/credentials.less"
        ]
    );
}

