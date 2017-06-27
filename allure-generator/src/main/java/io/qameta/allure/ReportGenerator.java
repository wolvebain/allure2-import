package io.qameta.allure;

import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 */
public class ReportGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportGenerator.class);

    private final Configuration configuration;

    public ReportGenerator(final Configuration configuration) {
        this.configuration = configuration;
    }

    public LaunchResults readResults(final Path resultsDirectory) {
        final DefaultResultsVisitor visitor = new DefaultResultsVisitor(configuration);
        configuration
                .getReaders()
                .forEach(reader -> reader.readResults(configuration, visitor, resultsDirectory));
        return visitor.getLaunchResults();
    }

    public void aggregate(final List<LaunchResults> results, final Path outputDirectory) throws IOException {
        for (Aggregator aggregator : configuration.getAggregators()) {
            aggregator.aggregate(configuration, results, outputDirectory);
        }
    }

    public void generate(final Path outputDirectory, final List<Path> resultsDirectories) throws IOException {
        generate(outputDirectory, resultsDirectories.stream());
    }

    public void generate(final Path outputDirectory, final Path... resultsDirectories) throws IOException {
        generate(outputDirectory, Stream.of(resultsDirectories));
    }

    private void generate(final Path outputDirectory, final Stream<Path> resultsDirectories) throws IOException {
        final List<LaunchResults> results = resultsDirectories
                .filter(this::isValidResultsDirectory)
                .map(this::readResults)
                .collect(Collectors.toList());
        aggregate(results, outputDirectory);
    }

    private boolean isValidResultsDirectory(final Path resultsDirectory) {
        if (Files.notExists(resultsDirectory)) {
            LOGGER.warn("{} does not exists", resultsDirectory);
            return false;
        }
        if (!Files.isDirectory(resultsDirectory)) {
            LOGGER.warn("{} is not a directory", resultsDirectory);
            return false;
        }
        return true;
    }
}
