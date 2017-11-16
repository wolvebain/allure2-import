package io.qameta.allure;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.qameta.allure.command.GenerateCommand;
import io.qameta.allure.command.MainCommand;
import io.qameta.allure.command.OpenCommand;
import io.qameta.allure.command.PluginCommand;
import io.qameta.allure.command.ServeCommand;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Artem Eroshenko <eroshenkoam@qameta.io>
 */
public class CommandLine {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);

    public static final String PROGRAM_NAME = "allure";
    public static final String SERVE_COMMAND = "serve";
    public static final String GENERATE_COMMAND = "generate";
    public static final String OPEN_COMMAND = "open";
    public static final String PLUGIN_COMMAND = "plugin";

    private final MainCommand mainCommand;
    private final ServeCommand serveCommand;
    private final GenerateCommand generateCommand;
    private final OpenCommand openCommand;
    private final PluginCommand pluginCommand;
    private final Commands commands;
    private final JCommander commander;

    public CommandLine(final Path allureHome) {
        this(new Commands(allureHome));
    }

    public CommandLine(final Commands commands) {
        this.commands = commands;
        this.mainCommand = new MainCommand();
        this.serveCommand = new ServeCommand();
        this.generateCommand = new GenerateCommand();
        this.openCommand = new OpenCommand();
        this.pluginCommand = new PluginCommand();
        this.commander = new JCommander(mainCommand);
        this.commander.addCommand(GENERATE_COMMAND, generateCommand);
        this.commander.addCommand(SERVE_COMMAND, serveCommand);
        this.commander.addCommand(OPEN_COMMAND, openCommand);
        this.commander.addCommand(PLUGIN_COMMAND, pluginCommand);
        this.commander.setProgramName(PROGRAM_NAME);
    }

    public static void main(final String[] args) throws InterruptedException {
        final String allureHome = System.getenv("APP_HOME");
        final CommandLine commandLine;
        if (Objects.isNull(allureHome)) {
            LOGGER.info("APP_HOME is not set, using default configuration");
            commandLine = new CommandLine((Path) null);
        } else {
            commandLine = new CommandLine(Paths.get(allureHome));
        }
        final ExitCode exitCode = commandLine
                .parse(args)
                .orElseGet(commandLine::run);
        System.exit(exitCode.getCode());
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public Optional<ExitCode> parse(final String... args) {
        if (args.length == 0) {
            printUsage(commander);
            return Optional.of(ExitCode.ARGUMENT_PARSING_ERROR);
        }
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            LOGGER.debug("Error during arguments parsing: {}", e);
            LOGGER.info("Could not parse arguments: {}", e.getMessage());
            printUsage(commander);
            return Optional.of(ExitCode.ARGUMENT_PARSING_ERROR);
        }

        //Hack to limit count of main parameters
        final List<Path> reportDirectories = openCommand.getReportDirectories();
        if (reportDirectories.size() != 1) {
            LOGGER.error("Only one main argument is allowed");
            return Optional.of(ExitCode.ARGUMENT_PARSING_ERROR);
        }

        return Optional.empty();
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public ExitCode run() {
        if (mainCommand.getVerboseOptions().isQuiet()) {
            LogManager.getRootLogger().setLevel(Level.OFF);
        }

        if (mainCommand.getVerboseOptions().isVerbose()) {
            LogManager.getRootLogger().setLevel(Level.DEBUG);
        }

        if (mainCommand.isVersion()) {
            String toolVersion = CommandLine.class.getPackage().getImplementationVersion();
            LOGGER.info(Objects.isNull(toolVersion) ? "unknown" : toolVersion);
            return ExitCode.NO_ERROR;
        }

        if (mainCommand.isHelp()) {
            printUsage(commander);
            return ExitCode.NO_ERROR;
        }

        final String parsedCommand = commander.getParsedCommand();
        if (Objects.isNull(parsedCommand)) {
            printUsage(commander);
            return ExitCode.ARGUMENT_PARSING_ERROR;
        }
        switch (parsedCommand) {
            case GENERATE_COMMAND:
                return commands.generate(
                        generateCommand.getReportDirectory(),
                        generateCommand.getResultsOptions().getResultsDirectories(),
                        generateCommand.isCleanReportDirectory(),
                        generateCommand.getConfigOptions()
                );
            case SERVE_COMMAND:
                return commands.serve(
                        serveCommand.getResultsOptions().getResultsDirectories(),
                        serveCommand.getHostPortOptions().getHost(),
                        serveCommand.getHostPortOptions().getPort(),
                        serveCommand.getConfigOptions());
            case OPEN_COMMAND:
                return commands.open(
                        openCommand.getReportDirectories().get(0),
                        openCommand.getHostPortOptions().getHost(),
                        openCommand.getHostPortOptions().getPort()
                );
            case PLUGIN_COMMAND:
                return commands.listPlugins(pluginCommand.getConfigOptions());
            default:
                printUsage(commander);
                return ExitCode.ARGUMENT_PARSING_ERROR;
        }
    }

    public JCommander getCommander() {
        return commander;
    }

    public MainCommand getMainCommand() {
        return mainCommand;
    }

    private void printUsage(final JCommander commander) {
        final String parsedCommand = commander.getParsedCommand();
        if (Objects.isNull(parsedCommand)) {
            commander.usage();
        } else {
            commander.usage(parsedCommand);
        }
    }
}
