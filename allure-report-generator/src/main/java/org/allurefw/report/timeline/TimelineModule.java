package org.allurefw.report.timeline;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import org.allurefw.report.ReportDataProvider;
import org.allurefw.report.TestCaseProcessor;
import org.allurefw.report.TimelineData;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 01.02.16
 */
public class TimelineModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TimelineData.class).in(Scopes.SINGLETON);

        Multibinder.newSetBinder(binder(), TestCaseProcessor.class)
                .addBinding().to(TimelinePlugin.class);

        Multibinder.newSetBinder(binder(), ReportDataProvider.class)
                .addBinding().to(TimelineDataProvider.class);
    }
}
