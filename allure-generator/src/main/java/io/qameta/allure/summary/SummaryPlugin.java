package io.qameta.allure.summary;

import io.qameta.allure.CommonJsonAggregator;
import io.qameta.allure.CompositeAggregator;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.GroupTime;
import io.qameta.allure.entity.Statistic;

import java.util.Arrays;
import java.util.List;

/**
 * Plugins generates Summary widget and summary export bean.
 *
 * @since 2.0
 */
@SuppressWarnings("PMD.UseUtilityClass")
public class SummaryPlugin extends CompositeAggregator {

    /** Name of the json file. */
    protected static final String JSON_FILE_NAME = "summary.json";

    public SummaryPlugin() {
        super(Arrays.asList(
                new JsonAggregator(), new WidgetAggregator()
        ));
    }

    protected static SummaryData getSummaryData(final List<LaunchResults> launches) {
        final SummaryData data = new SummaryData()
                .setStatistic(new Statistic())
                .setTime(new GroupTime())
                .setReportName("Allure Report");
        launches.stream()
                .flatMap(launch -> launch.getResults().stream())
                .forEach(result -> {
                    data.getStatistic().update(result);
                    data.getTime().update(result);
                });
        return data;
    }

    private static class JsonAggregator extends CommonJsonAggregator {

        JsonAggregator() {
            super(JSON_FILE_NAME);
        }

        @Override
        protected SummaryData getData(final List<LaunchResults> launches) {
            return SummaryPlugin.getSummaryData(launches);
        }
    }

    private static class WidgetAggregator extends CommonJsonAggregator {

        WidgetAggregator() {
            super("widgets", JSON_FILE_NAME);
        }

        @Override
        protected SummaryData getData(final List<LaunchResults> launches) {
            return SummaryPlugin.getSummaryData(launches);
        }
    }
}
