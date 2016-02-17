package org.allurefw.report;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.allurefw.report.allure1.Allure1Module;
import org.allurefw.report.behaviors.BehaviorsModule;
import org.allurefw.report.config.ConfigModule;
import org.allurefw.report.defects.DefectsModule;
import org.allurefw.report.environment.EnvironmentModule;
import org.allurefw.report.graph.GraphModule;
import org.allurefw.report.jackson.JacksonMapperModule;
import org.allurefw.report.junit.JunitModule;
import org.allurefw.report.report.ReportModule;
import org.allurefw.report.results.ResultsModule;
import org.allurefw.report.timeline.TimelineModule;
import org.allurefw.report.widgets.WidgetsModule;
import org.allurefw.report.writer.WriterModule;
import org.allurefw.report.xunit.XunitModule;

import java.nio.file.Path;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 30.01.16
 */
public class ReportGenerator {

    private final Injector injector;

    public ReportGenerator(Path... inputs) {
        injector = Guice.createInjector(
//                Core
                new BootstrapModule(inputs),
                new ConfigModule(),
                new ResultsModule(),
                new JacksonMapperModule(),
                new WriterModule(),
                new WidgetsModule(),
                new ReportModule(),
                new EnvironmentModule(),
//                Readers
                new Allure1Module(),
                new JunitModule(),
//                Tabs
                new DefectsModule(),
                new XunitModule(),
                new BehaviorsModule(),
                new TimelineModule(),
                new GraphModule()
        );
    }

    public void generate(Path output) {
        injector.getInstance(Lifecycle.class).generate(output);
    }
}
