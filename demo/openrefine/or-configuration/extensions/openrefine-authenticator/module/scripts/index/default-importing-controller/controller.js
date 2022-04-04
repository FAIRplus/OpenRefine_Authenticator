/*
    Custom code from original OpenRefine project main/webapp/modules/core/scripts/index/default-importing-controller/controller.js
*/

Refine.DefaultImportingController.prototype.startImportJob = function (form, progressMessage, callback) {
    var self = this;

    $(form).find('input:text').filter(function () {
        return this.value === "";
    }).prop("disabled", true);
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
    Refine.wrapCSRF(function (token) {
        $.post(
            "command/core/create-importing-job",
            {csrf_token: token},
            function (data) {
                var jobID = self._jobID = data.jobID;

                form.attr("method", "post")
                    .attr("enctype", "multipart/form-data")
                    .attr("accept-charset", "UTF-8")
                    .attr("target", "create-project-iframe")
                    .attr("action", "command/core/importing-controller?" + $.param({
                        "controller": "openrefine-authenticator/default-importing-controller",
                        "jobID": jobID,
                        "subCommand": "load-raw-data",
                        "csrf_token": token,
                        "authenticatorToken": authenticatorToken,
                        "authenticatorHeader": authenticatorHeader
                    }));
                form[0].submit();

                var start = new Date();
                var timerID = window.setInterval(
                    function () {
                        self._createProjectUI.pollImportJob(
                            start, jobID, timerID,
                            function (job) {
                                return job.config.hasData;
                            },
                            function (jobID, job) {
                                self._job = job;
                                self._onImportJobReady();
                                if (callback) {
                                    callback(jobID, job);
                                }
                            },
                            function (job) {
                                alert(job.config.error + '\n' + job.config.errorDetails);
                                self._startOver();
                            }
                        );
                    },
                    1000
                );
                self._createProjectUI.showImportProgressPanel(progressMessage, function () {
                    // stop the iframe
                    $('#create-project-iframe')[0].contentWindow.stop();

                    // stop the timed polling
                    window.clearInterval(timerID);

                    // explicitly cancel the import job
                    Refine.CreateProjectUI.cancelImportingJob(jobID);

                    self._createProjectUI.showSourceSelectionPanel();
                });
            },
            "json"
        );
    });
};