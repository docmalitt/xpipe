package io.xpipe.app.core.window;

import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.*;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.InputHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AppWindowHelper {

    public static Node alertContentText(String s) {
        var text = new Text(s);
        text.setWrappingWidth(450);
        AppFont.medium(text);
        var sp = new StackPane(text);
        sp.setPadding(new Insets(5));
        return sp;
    }

    public static void addIcons(Stage stage) {
        stage.getIcons().clear();

        // This allows for assigning logos even if AppImages has not been initialized yet
        var dir = OsType.getLocal() == OsType.MACOS ? "img/logo/padded" : "img/logo/full";
        AppResources.with(AppResources.XPIPE_MODULE, dir, path -> {
            var size =
                    switch (OsType.getLocal()) {
                        case OsType.Linux linux -> 128;
                        case OsType.MacOs macOs -> 128;
                        case OsType.Windows windows -> 32;
                    };
            stage.getIcons().add(AppImages.loadImage(path.resolve("logo_" + size + "x" + size + ".png")));
        });
    }

    public static Stage sideWindow(
            String title, Function<Stage, Comp<?>> contentFunc, boolean bindSize, ObservableValue<Boolean> loading) {
        var stage = AppWindowBounds.centerStage();
        stage.initStyle(StageStyle.UNIFIED);
        if (AppMainWindow.getInstance() != null) {
            stage.initOwner(AppMainWindow.getInstance().getStage());
        }
        stage.setTitle(title);

        addIcons(stage);
        setupContent(stage, contentFunc, bindSize, loading);
        setupStylesheets(stage.getScene());
        AppWindowBounds.fixInvalidStagePosition(stage);

        if (AppPrefs.get() != null && AppPrefs.get().enforceWindowModality().get()) {
            stage.initModality(Modality.WINDOW_MODAL);
        }

        stage.setOnShown(e -> {
            AppTheme.initThemeHandlers(stage);
        });
        return stage;
    }

    public static void showAlert(Consumer<Alert> c, Consumer<Optional<ButtonType>> bt) {
        ThreadHelper.runAsync(() -> {
            var r = showBlockingAlert(c);
            if (bt != null) {
                bt.accept(r);
            }
        });
    }

    public static void setContent(Alert alert, String s) {
        alert.getDialogPane().setMinWidth(505);
        alert.getDialogPane().setPrefWidth(505);
        alert.getDialogPane().setMaxWidth(505);
        alert.getDialogPane().setContent(AppWindowHelper.alertContentText(s));
    }

    public static boolean showConfirmationAlert(String title, String header, String content) {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.titleProperty().bind(AppI18n.observable(title));
                    alert.headerTextProperty().bind(AppI18n.observable(header));
                    setContent(alert, AppI18n.get(content));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    public static boolean showConfirmationAlert(
            ObservableValue<String> title, ObservableValue<String> header, ObservableValue<String> content) {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.titleProperty().bind(title);
                    alert.headerTextProperty().bind(header);
                    setContent(alert, content.getValue());
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                })
                .map(b -> b.getButtonData().isDefaultButton())
                .orElse(false);
    }

    public static Optional<ButtonType> showBlockingAlert(Consumer<Alert> c) {
        Supplier<Alert> supplier = () -> {
            Alert a = AppWindowHelper.createEmptyAlert();
            AppFont.normal(a.getDialogPane());
            var s = (Stage) a.getDialogPane().getScene().getWindow();
            s.setOnShown(event -> {
                Platform.runLater(() -> {
                    AppWindowBounds.clampWindow(s).ifPresent(rectangle2D -> {
                        s.setX(rectangle2D.getMinX());
                        s.setY(rectangle2D.getMinY());
                        // Somehow we have to set max size as setting the normal size does not work?
                        s.setMaxWidth(rectangle2D.getWidth());
                        s.setMaxHeight(rectangle2D.getHeight());
                    });
                });
                event.consume();
            });
            AppWindowBounds.fixInvalidStagePosition(s);
            a.getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode().equals(KeyCode.W) && event.isShortcutDown()) {
                    s.close();
                    event.consume();
                    return;
                }

                if (event.getCode().equals(KeyCode.ESCAPE)) {
                    s.close();
                    event.consume();
                }
            });
            return a;
        };

        AtomicReference<Optional<ButtonType>> result = new AtomicReference<>();
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    Alert a = supplier.get();
                    c.accept(a);
                    result.set(a.showAndWait());
                } catch (Throwable t) {
                    result.set(Optional.empty());
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            Alert a = supplier.get();
            c.accept(a);
            result.set(a.showAndWait());
        }
        return result.get();
    }

    public static Alert createEmptyAlert() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        if (AppMainWindow.getInstance() != null) {
            alert.initOwner(AppMainWindow.getInstance().getStage());
        }
        alert.getDialogPane().getScene().setFill(Color.TRANSPARENT);
        addIcons(((Stage) alert.getDialogPane().getScene().getWindow()));
        setupStylesheets(alert.getDialogPane().getScene());
        return alert;
    }

    public static void setupStylesheets(Scene scene) {
        AppStyle.addStylesheets(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isDeveloperMode() && event.getCode().equals(KeyCode.F3)) {
                AppStyle.reloadStylesheets(scene);
                TrackEvent.debug("Reloaded stylesheets");
                event.consume();
            }
        });
        TrackEvent.debug("Set stylesheet reload listener");

        InputHelper.onNavigationInput(scene, (kb) -> {
            var r = scene.getRoot();
            if (r != null) {
                var acc = Platform.isAccessibilityActive();
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("key-navigation"), kb && !acc);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("normal-navigation"), !kb && !acc);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("accessibility-navigation"), acc);
            }
        });

        Platform.accessibilityActiveProperty().addListener((observable, oldValue, newValue) -> {
            var r = scene.getRoot();
            if (r != null) {
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("key-navigation"), false);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("normal-navigation"), false);
                r.pseudoClassStateChanged(PseudoClass.getPseudoClass("accessibility-navigation"), true);
            }
        });
    }

    public static void setupContent(
            Stage stage, Function<Stage, Comp<?>> contentFunc, boolean bindSize, ObservableValue<Boolean> loading) {
        var baseComp = contentFunc.apply(stage);
        var content = loading != null ? LoadingOverlayComp.noProgress(baseComp, loading) : baseComp;
        var contentR = content.createRegion();
        AppFont.small(contentR);
        var scene = new Scene(bindSize ? new Pane(contentR) : contentR, -1, -1, false);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        contentR.requestFocus();
        if (bindSize) {
            bindSize(stage, contentR);
            stage.setResizable(false);
        }

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (AppProperties.get().isDeveloperMode() && event.getCode().equals(KeyCode.F6)) {
                var newBaseComp = contentFunc.apply(stage);
                var newComp = loading != null ? LoadingOverlayComp.noProgress(newBaseComp, loading) : newBaseComp;
                var newR = newComp.createRegion();
                AppFont.medium(newR);
                scene.setRoot(bindSize ? new Pane(newR) : newR);
                newR.requestFocus();
                if (bindSize) {
                    bindSize(stage, newR);
                }

                TrackEvent.debug("Rebuilt content");
                event.consume();
            }
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode().equals(KeyCode.W) && event.isShortcutDown()) {
                stage.close();
                event.consume();
            }
        });
    }

    private static void bindSize(Stage stage, Region r) {
        if (r.getPrefWidth() == Region.USE_COMPUTED_SIZE) {
            r.widthProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        } else {
            stage.setWidth(r.getPrefWidth());
            r.prefWidthProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        }

        if (r.getPrefHeight() == Region.USE_COMPUTED_SIZE) {
            r.heightProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        } else {
            stage.setHeight(r.getPrefHeight());
            r.prefHeightProperty().addListener((c, o, n) -> {
                stage.sizeToScene();
            });
        }

        stage.sizeToScene();
    }
}
