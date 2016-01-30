package org.allurefw.report.allure1;

import org.allurefw.Status;
import org.allurefw.report.Attachment;
import org.allurefw.report.Failure;
import org.allurefw.report.Parameter;
import org.allurefw.report.Step;
import org.allurefw.report.TestCase;
import org.allurefw.report.Time;
import ru.yandex.qatools.allure.model.DescriptionType;
import ru.yandex.qatools.allure.model.ParameterKind;
import ru.yandex.qatools.allure.model.TestCaseResult;
import ru.yandex.qatools.allure.model.TestSuiteResult;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.allurefw.report.ReportApiUtils.processMarkdown;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 08.10.15
 */
public class TestCaseResultIterator2 implements Iterator<TestCase> {

    private final Iterator<TestCaseResult> iterator;

    public TestCaseResultIterator2(TestSuiteResult testSuite) {
        this.iterator = testSuite.getTestCases().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestCase next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        TestCaseResult result = iterator.next();

        //TODO add information about suite
        return convert(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private TestCase convert(TestCaseResult source) {
        TestCase dest = new TestCase();
        dest.setName(source.getTitle() != null ? source.getTitle() : source.getName());
        dest.setStatus(convertStatus(source.getStatus()));

        if (source.getDescription() != null) {
            dest.setDescription(source.getDescription().getValue());
            dest.setDescriptionHtml(source.getDescription().getType() == DescriptionType.HTML
                    ? source.getDescription().getValue()
                    : processMarkdown(source.getDescription().getValue())
            );
        }

        if (source.getFailure() != null) {
            dest.setFailure(new Failure()
                    .withMessage(source.getFailure().getMessage())
                    .withTrace(source.getFailure().getStackTrace())
            );
        }
        dest.setTime(new Time()
                .withStart(source.getStart())
                .withStop(source.getStop())
                .withDuration(source.getStop() - source.getStart())
        );
        dest.setParameters(source.getParameters().stream()
                .filter(parameter -> ParameterKind.ARGUMENT.equals(parameter.getKind()))
                .map(parameter -> new Parameter()
                        .withName(parameter.getName())
                        .withValue(parameter.getValue()))
                .collect(Collectors.toList())
        );
        dest.setSteps(convertSteps(source.getSteps()));
        dest.setAttachments(convertAttachments(source.getAttachments()));
        return dest;
    }

    private List<Step> convertSteps(
            List<ru.yandex.qatools.allure.model.Step> steps) {
        return steps.stream()
                .map(s -> new Step()
                        .withName(s.getTitle())
                        .withStatus(convertStatus(s.getStatus()))
                        .withSteps(convertSteps(s.getSteps()))
                        .withAttachments(convertAttachments(s.getAttachments())))
                .collect(Collectors.toList());
    }

    private List<Attachment> convertAttachments(
            List<ru.yandex.qatools.allure.model.Attachment> attachments) {
        return attachments.stream()
                .map(a -> new Attachment()
                        .withName(a.getTitle())
                        .withSource(a.getSource())
                        .withType(a.getType()))
                .collect(Collectors.toList());
    }

    private Status convertStatus(
            ru.yandex.qatools.allure.model.Status status) {
        try {
            return Status.fromValue(status.value());
        } catch (Exception ignored) {
            //convert skipped to canceled
            return Status.CANCELED;
        }
    }

}
