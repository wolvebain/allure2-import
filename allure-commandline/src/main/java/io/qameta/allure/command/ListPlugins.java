package io.qameta.allure.command;

import com.github.rvesse.airline.annotations.Command;

import javax.inject.Inject;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
@Command(name = "plugins", description = "Show installed plugins")
public class ListPlugins implements AllureCommand {

    @Inject
    private final VerboseOptions verboseOptions = new VerboseOptions();

    @Override
    public void run(final Context context) throws Exception {
        verboseOptions.configureLogLevel();
    }
}
