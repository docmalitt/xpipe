package io.xpipe.app.prefs;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SyncCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "sync";
    }

    public Comp<?> create() {
        var prefs = AppPrefs.get();
        AtomicReference<Region> button = new AtomicReference<>();
        var terminalTest = new StackComp(
                List.of(new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
                    ThreadHelper.runAsync(() -> {
                        var r = DataStorageSyncHandler.getInstance().validateConnection();
                        if (r) {
                            Platform.runLater(() -> {
                                button.get().getStyleClass().add(Styles.SUCCESS);
                            });
                        }
                    });
                }).apply(struc -> button.set(struc.get())).padding(new Insets(6, 10, 6, 6))))
                .padding(new Insets(10, 0, 0, 0))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
        var builder = new OptionsBuilder();
        builder.addTitle("sync")
                .sub(new OptionsBuilder()
                        .name("enableGitStorage")
                        .description("enableGitStorageDescription")
                        .addToggle(prefs.enableGitStorage)
                        .nameAndDescription("storageGitRemote")
                        .addString(prefs.storageGitRemote)
                        .disable(prefs.enableGitStorage.not())
                        .addComp(terminalTest)
                        .disable(prefs.storageGitRemote.isNull().or(prefs.enableGitStorage.not()))
                        .addComp(prefs.getCustomComp("gitVaultIdentityStrategy"))
                        .nameAndDescription("openDataDir")
                        .addComp(new ButtonComp(AppI18n.observable("openDataDirButton"), () -> {
                            DesktopHelper.browsePathLocal(DataStorage.get().getDataDir());
                        })));
        return builder.buildComp();
    }
}
