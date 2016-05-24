package org.allurefw.report.environment;

import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.allurefw.report.AbstractPlugin;
import org.allurefw.report.Environment;
import org.allurefw.report.Plugin;
import org.allurefw.report.ResultsDirectories;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.allurefw.report.ReportApiUtils.loadProperties;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 17.02.16
 */
@Plugin(name = "environment")
public class EnvironmentPlugin extends AbstractPlugin {

    @Override
    protected void configure() {
        aggregator(NoopAggregator.class)
                .toWidget(getPluginName(), EnvironmentFinalizer.class);
    }

    @Provides
    @Singleton
    public Environment provide(@ResultsDirectories Path... resultsDirectories) {
        Properties defaults = new Properties();
        //TODO is there the right place for the defaults?
        defaults.put("allure.test.run.name", "Allure Test Pack");
        Properties properties = loadProperties(defaults, "environment.properties", resultsDirectories);
        Map<String, String> map = new HashMap<>(Maps.fromProperties(properties));
        String id = map.remove("allure.test.run.id");
        String name = map.remove("allure.test.run.name");
        String url = map.remove("allure.test.run.url");

        Map<String, String> parameters = Collections.unmodifiableMap(map);
        return new DefaultEnvironment(id, name, url, parameters);
    }
}
