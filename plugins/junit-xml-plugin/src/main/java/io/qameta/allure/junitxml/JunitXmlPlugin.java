package io.qameta.allure.junitxml;

import io.qameta.allure.Reader;
import io.qameta.allure.context.RandomUidContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.ResultsVisitor;
import io.qameta.allure.entity.LabelName;
import io.qameta.allure.entity.StageResult;
import io.qameta.allure.entity.Status;
import io.qameta.allure.entity.StatusDetails;
import io.qameta.allure.entity.TestResult;
import io.qameta.allure.entity.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.file.Files.newDirectoryStream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Plugin that reads data in JUnit.xml format.
 *
 * @since 2.0
 */
public class JunitXmlPlugin implements Reader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JunitXmlPlugin.class);

    private static final BigDecimal MULTIPLICAND = new BigDecimal(1000);

    private static final String TEST_SUITE_ELEMENT_NAME = "testsuite";
    private static final String TEST_CASE_ELEMENT_NAME = "testcase";
    private static final String SKIPPED_ELEMENT_NAME = "skipped";
    private static final String FAILURE_ELEMENT_NAME = "failure";
    private static final String ERROR_ELEMENT_NAME = "error";
    private static final String MESSAGE_ELEMENT_NAME = "message";
    private static final String TIME_ATTRIBUTE_NAME = "time";

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    @Override
    public void readResults(final Configuration configuration, final ResultsVisitor visitor, final Path directory) {
        final RandomUidContext context = configuration.requireContext(RandomUidContext.class);
        listResults(directory).forEach(result -> parseTestSuite(directory, result, context, visitor));
    }

    private void parseTestSuite(final Path resultsDirectory, final Path parsedFile,
                                final RandomUidContext context, final ResultsVisitor visitor) {
        try {
            LOGGER.debug("Parsing file {}", parsedFile);
            final XmlElement testSuiteElement = Xmlwise.loadXml(parsedFile.toFile());
            final String elementName = testSuiteElement.getName();
            if (!TEST_SUITE_ELEMENT_NAME.equals(elementName)) {
                LOGGER.debug("File {} is not a valid JUnit xml. Unknown root element {}", parsedFile, elementName);
                return;
            }

            testSuiteElement.get(TEST_CASE_ELEMENT_NAME)
                    .forEach(element -> parseTestCase(element, resultsDirectory, parsedFile, context, visitor));
        } catch (XmlParseException | IOException e) {
            LOGGER.error("Could not parse file {}: {}", parsedFile, e);
        }
    }

    private void parseTestCase(final XmlElement testCaseElement, final Path resultsDirectory,
                               final Path parsedFile, final RandomUidContext context, final ResultsVisitor visitor) {
        final String className = testCaseElement.getAttribute("classname");
        final String name = testCaseElement.getAttribute("name");
        final Status status = getStatus(testCaseElement);
        final String historyId = String.format("%s#%s", className, name);
        final TestResult result = new TestResult();
        if (nonNull(className) && nonNull(name)) {
            result.setHistoryId(historyId);
        }
        result.setUid(context.getValue().get());
        result.setName(isNull(name) ? "Unknown test case" : name);
        result.setStatus(status);
        result.setStatusDetails(getStatusDetails(testCaseElement));
        result.setTime(getTime(testCaseElement, parsedFile));

        final Path attachmentFile = resultsDirectory.resolve(className + ".txt");
        Optional.of(attachmentFile)
                .filter(Files::exists)
                .map(visitor::visitAttachmentFile)
                .map(attachment1 -> attachment1.withName("System out"))
                .ifPresent(attachment -> result.setTestStage(new StageResult().withAttachments(attachment)));

        if (nonNull(className)) {
            result.addLabelIfNotExists(LabelName.SUITE, className);
            result.addLabelIfNotExists(LabelName.TEST_CLASS, className);
            result.addLabelIfNotExists(LabelName.PACKAGE, className);
        }

        visitor.visitTestResult(result);
    }

    private Status getStatus(final XmlElement testCaseElement) {
        if (testCaseElement.contains(FAILURE_ELEMENT_NAME)) {
            return Status.FAILED;
        }
        if (testCaseElement.contains(ERROR_ELEMENT_NAME)) {
            return Status.BROKEN;
        }
        if (testCaseElement.contains(SKIPPED_ELEMENT_NAME)) {
            return Status.SKIPPED;
        }
        return Status.PASSED;
    }

    private StatusDetails getStatusDetails(final XmlElement testCaseElement) {
        final boolean flaky = isFlaky(testCaseElement);
        return Stream.of(FAILURE_ELEMENT_NAME, ERROR_ELEMENT_NAME, SKIPPED_ELEMENT_NAME)
                .filter(testCaseElement::contains)
                .map(testCaseElement::get)
                .filter(elements -> !elements.isEmpty())
                .flatMap(Collection::stream)
                .findFirst()
                .map(element -> new StatusDetails()
                        .withMessage(element.getAttribute(MESSAGE_ELEMENT_NAME))
                        .withTrace(element.getValue())
                        .withFlaky(flaky))
                .orElseGet(() -> new StatusDetails().withFlaky(flaky));
    }

    private Time getTime(final XmlElement testCaseElement, final Path parsedFile) {
        if (testCaseElement.containsAttribute(TIME_ATTRIBUTE_NAME)) {
            try {
                final long duration = BigDecimal.valueOf(testCaseElement.getDoubleAttribute(TIME_ATTRIBUTE_NAME))
                        .multiply(MULTIPLICAND)
                        .longValue();
                return new Time().withDuration(duration);
            } catch (XmlParseException e) {
                LOGGER.debug(
                        "Could not parse time attribute for element {} in file {}",
                        testCaseElement, parsedFile, e
                );
            }
        }
        return new Time();
    }

    private boolean isFlaky(final XmlElement testCaseElement) {
        return testCaseElement.contains("rerunError") || testCaseElement.contains("rerunFailure");
    }

    private static List<Path> listResults(final Path directory) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }

        try (DirectoryStream<Path> directoryStream = newDirectoryStream(directory, "TEST-*.xml")) {
            for (Path path : directoryStream) {
                if (!Files.isDirectory(path)) {
                    result.add(path);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not read data from {}: {}", directory, e);
        }
        return result;
    }
}
