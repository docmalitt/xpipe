package io.xpipe.core.impl;

import io.xpipe.core.process.ShellProcessControl;

import java.util.ServiceLoader;

public abstract class LocalProcessControlProvider {

    private static LocalProcessControlProvider INSTANCE;

    public static void init(ModuleLayer layer) {
        INSTANCE = layer != null
                ? ServiceLoader.load(layer, LocalProcessControlProvider.class)
                        .findFirst()
                        .orElse(null)
                : ServiceLoader.load(LocalProcessControlProvider.class)
                        .findFirst()
                        .orElse(null);
    }

    public static ShellProcessControl create() {
        return INSTANCE.createProcessControl();
    }

    public abstract ShellProcessControl createProcessControl();
}
