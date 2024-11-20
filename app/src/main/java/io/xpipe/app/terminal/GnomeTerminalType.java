package io.xpipe.app.terminal;

import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.util.FailableFunction;

public class GnomeTerminalType extends ExternalTerminalType.PathCheckType implements TrackableTerminalType {

    public GnomeTerminalType() {
        super("app.gnomeTerminal", "gnome-terminal", true);
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public String getWebsite() {
        return "https://help.gnome.org/users/gnome-terminal/stable/";
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean supportsColoredTitle() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (ShellControl pc = LocalShell.getShell()) {
            CommandSupport.isInPathOrThrow(pc, executable, toTranslatedString().getValue(), null);

            var toExecute = CommandBuilder.of()
                    .add(executable, "-v", "--title")
                    .addQuoted(configuration.getColoredTitle())
                    .add("--")
                    .addFile(configuration.getScriptFile())
                    // In order to fix this bug which also affects us:
                    // https://askubuntu.com/questions/1148475/launching-gnome-terminal-from-vscode
                    .envrironment("GNOME_TERMINAL_SCREEN", sc -> "");
            pc.executeSimpleCommand(toExecute);
        }
    }

    @Override
    public FailableFunction<TerminalLaunchConfiguration, String, Exception> remoteLaunchCommand(
            ShellDialect systemDialect) {
        return launchConfiguration -> {
            var toExecute = CommandBuilder.of()
                    .add(executable, "-v", "--title")
                    .addQuoted(launchConfiguration.getColoredTitle())
                    .add("--")
                    .addFile(launchConfiguration.getScriptFile());
            return toExecute.buildSimple();
        };
    }
}