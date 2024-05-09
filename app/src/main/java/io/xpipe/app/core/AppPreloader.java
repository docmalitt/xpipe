package io.xpipe.app.core;

import io.xpipe.app.issue.TrackEvent;
import javafx.application.Preloader;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class AppPreloader extends Preloader {

    @Override
    @SneakyThrows
    public void start(Stage primaryStage) {
        // Do it this way to prevent IDE inspections from complaining
        var c = Class.forName(
                ModuleLayer.boot().findModule("javafx.graphics").orElseThrow(), "com.sun.glass.ui.Application");
        var m = c.getDeclaredMethod("setName", String.class);
        m.invoke(c.getMethod("GetApplication").invoke(null), "XPipe");
        TrackEvent.info("Application preloaded launched");
    }
}
