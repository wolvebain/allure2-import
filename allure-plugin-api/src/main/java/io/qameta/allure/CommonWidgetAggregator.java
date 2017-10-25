package io.qameta.allure;

import io.qameta.allure.context.JacksonContext;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class CommonWidgetAggregator implements Aggregator {

    private final String fileName;

    protected CommonWidgetAggregator(final String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void aggregate(final Configuration configuration,
                          final List<LaunchResults> launchesResults,
                          final Path outputDirectory) throws IOException {
        final JacksonContext context = configuration.requireContext(JacksonContext.class);
        final Path widgetsFolder = Files.createDirectories(outputDirectory.resolve("widgets"));
        final Path widgetFile = widgetsFolder.resolve(this.fileName);
        try (OutputStream os = Files.newOutputStream(widgetFile)) {
            context.getValue().writeValue(os, getData(configuration, launchesResults));
        }
    }

    protected abstract Object getData(final Configuration configuration, final List<LaunchResults> launches);

    public static class WidgetCollection<T> {

        private final long total;

        private final List<T> items;

        public WidgetCollection(final long total, final List<T> items) {
            this.total = total;
            this.items = items;
        }

        public long getTotal() {
            return total;
        }

        public List<T> getItems() {
            return items;
        }
    }
}
