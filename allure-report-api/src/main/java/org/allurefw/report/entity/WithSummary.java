package org.allurefw.report.entity;

import org.allurefw.report.Step;
import org.allurefw.report.Summary;

import java.util.function.BinaryOperator;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 31.01.16
 */
public interface WithSummary extends WithSteps, WithAttachments {

    BinaryOperator<Summary> reduceOperator = (first, second) -> new Summary()
            .withSteps(first.getSteps() + second.getSteps())
            .withAttachments(first.getAttachments() + second.getAttachments());

    default Summary getSummary() {
        Summary current = new Summary()
                .withSteps((long) getSteps().size())
                .withAttachments((long) getAttachments().size());

        return getSteps().stream()
                .map(Step::getSummary)
                .reduce(current, reduceOperator);
    }
}
