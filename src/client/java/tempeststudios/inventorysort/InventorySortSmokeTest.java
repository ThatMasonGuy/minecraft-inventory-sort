package tempeststudios.inventorysort;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import tempeststudios.inventorysort.core.InventorySortCore;

public final class InventorySortSmokeTest {
    private static final Logger LOGGER = InventorySortCore.LOGGER;
    private static final String SMOKE_TEST_PROPERTY = "inventorysort.smokeTest";
    private static final String[] FORCED_MIXIN_TARGETS = {
            "net.minecraft.client.multiplayer.MultiPlayerGameMode"
    };
    private static final int PASS_AFTER_TICKS = 20;
    private static final long DEFAULT_TIMEOUT_MILLIS = 180_000L;
    private static final String TIMEOUT_SECONDS_PROPERTY = "inventorysort.smokeTimeoutSeconds";

    private static int ticks;
    private static volatile boolean complete;

    private InventorySortSmokeTest() {
    }

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(SMOKE_TEST_PROPERTY)) {
            return;
        }

        LOGGER.info("Inventory Sort automated smoke test armed");
        startWatchdog();
        forceLoadMixinTargets();
        ClientTickEvents.END_CLIENT_TICK.register(InventorySortSmokeTest::tick);
    }

    private static void startWatchdog() {
        long timeoutMillis = timeoutMillis();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(timeoutMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (complete) {
                return;
            }

            LOGGER.error(
                    "INVENTORYSORT_SMOKE_TEST_TIMEOUT minecraftProfile={} releaseProfile={} installSet={} timeoutMillis={}",
                    System.getProperty("inventorysort.smokeMinecraftProfile", "unknown"),
                    System.getProperty("inventorysort.smokeReleaseProfile", "unknown"),
                    System.getProperty("inventorysort.smokeInstallSet", "unknown"),
                    timeoutMillis
            );
            Runtime.getRuntime().halt(124);
        }, "InventorySortSmokeTest-Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static long timeoutMillis() {
        String seconds = System.getProperty(TIMEOUT_SECONDS_PROPERTY);
        if (seconds == null || seconds.isBlank()) {
            return DEFAULT_TIMEOUT_MILLIS;
        }

        try {
            long parsedSeconds = Long.parseLong(seconds);
            if (parsedSeconds > 0L) {
                return parsedSeconds * 1_000L;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid {} value '{}'; using default smoke timeout", TIMEOUT_SECONDS_PROPERTY, seconds);
        }
        return DEFAULT_TIMEOUT_MILLIS;
    }

    private static void forceLoadMixinTargets() {
        ClassLoader classLoader = InventorySortSmokeTest.class.getClassLoader();
        for (String className : FORCED_MIXIN_TARGETS) {
            try {
                Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Smoke test could not load mixin target " + className, e);
            }
        }
    }

    private static void tick(Minecraft client) {
        if (complete) {
            return;
        }

        ticks++;
        if (ticks < PASS_AFTER_TICKS) {
            return;
        }

        complete = true;
        LOGGER.info(
                "INVENTORYSORT_SMOKE_TEST_PASS minecraftProfile={} releaseProfile={} installSet={} injectedMods={}",
                System.getProperty("inventorysort.smokeMinecraftProfile", "unknown"),
                System.getProperty("inventorysort.smokeReleaseProfile", "unknown"),
                System.getProperty("inventorysort.smokeInstallSet", "unknown"),
                System.getProperty("fabric.addMods", "unknown")
        );
        client.stop();
    }
}
