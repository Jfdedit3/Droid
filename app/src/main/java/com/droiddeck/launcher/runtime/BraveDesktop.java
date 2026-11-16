package com.droiddeck.launcher.runtime;

import android.content.Context;
import android.util.Log;
import com.droiddeck.launcher.core.BravePayload;
import com.droiddeck.launcher.core.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class BraveDesktop {
    public static final String VERSION = "1.99.57";
    public static final String ASSET = "bundled/brave-arm64.deb";
    public static final String SHA256 = "d05df4f8a8c494075246820c17897a469614f879938a77557f025fda7be61db4";
    private BraveDesktop() {}

    public static synchronized boolean install(Context context, File root) {
        File parent = new File(root, "opt/brave.com");
        File target = new File(parent, "brave");
        File staging = new File(parent, ".droiddeck-brave-staging");
        File previous = new File(parent, ".droiddeck-brave-previous");
        File marker = new File(target, ".droiddeck-version");
        try {
            if (!target.exists() && previous.exists() && !previous.renameTo(target))
                throw new IOException("Cannot recover the previous browser");
            if (VERSION.equals(FileUtils.readString(marker)) && new File(target, "brave").canExecute()) return true;
            FileUtils.delete(staging);
            if (!staging.mkdirs()) throw new IOException("Cannot stage Brave");
            try (InputStream in = context.getAssets().open(ASSET)) { BravePayload.extract(in, staging); }
            if (!FileUtils.writeString(new File(staging, ".droiddeck-version"), VERSION))
                throw new IOException("Cannot record browser version");
            FileUtils.delete(previous);
            if (target.exists() && !target.renameTo(previous)) throw new IOException("Cannot preserve previous browser");
            if (!staging.renameTo(target)) {
                if (previous.exists()) previous.renameTo(target);
                throw new IOException("Cannot install browser");
            }
            FileUtils.delete(previous);
            return true;
        } catch (Exception error) {
            Log.w("BraveDesktop", "Bundled browser installation failed; desktop remains available", error);
            return false;
        } finally {
            FileUtils.delete(staging);
        }
    }
}
