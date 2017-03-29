package io.qameta.allure.command;

import com.github.rvesse.airline.annotations.Command;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.qameta.allure.utils.CommandUtils.copyDirectory;
import static io.qameta.allure.utils.CommandUtils.createReportGenerator;
import static io.qameta.allure.utils.CommandUtils.openBrowser;
import static io.qameta.allure.utils.CommandUtils.setUpServer;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
@Command(name = "serve", description = "Serve the report")
public class ReportServe implements AllureCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportServe.class);

    @Inject
    private final ResultsOptions resultsOptions = new ResultsOptions();

    @Inject
    private final PortOptions portOptions = new PortOptions();

    @Inject
    private final VerboseOptions verboseOptions = new VerboseOptions();

    @Override
    public void run(final Context context) throws Exception {
        verboseOptions.configureLogLevel();

        Path serve = Files.createTempDirectory(context.getWorkDirectory(), "serve");
        LOGGER.info("Generate report to temp directory...");
        createReportGenerator(context)
                .generate(serve, resultsOptions.getResultsDirectories());
        copyDirectory(context.getWebDirectory(), serve);

        LOGGER.info("Starting web server...");
        Server server = setUpServer(portOptions.getPort(), serve);
        server.start();

        openBrowser(server.getURI());
        LOGGER.info("Server started at <{}>. Press <Ctrl+C> to exit", server.getURI());
        server.join();
    }
}
