package fr.minibosstimer;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Petit panneau en haut à gauche listant les timers en cours. */
public final class TimerHud {
    private static final int MAX_LINES = 10;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFAA00;
    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFFF55;

    private TimerHud() {}

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MiniBossTimerMod.MOD_ID, "timers"),
                TimerHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!Config.data.showHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        List<TimerManager.Timer> timers = TimerManager.snapshot();
        if (timers.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        lines.add("Mini-boss");
        colors.add(GOLD);

        int shown = 0;
        for (TimerManager.Timer t : timers) {
            if (shown++ >= MAX_LINES) break;
            long remaining = t.respawnAt - now;
            if (remaining <= 0) {
                lines.add("[" + t.server + "] " + t.boss + "  PRET !");
                colors.add(GREEN);
            } else {
                lines.add("[" + t.server + "] " + t.boss + "  " + Msg.duration(remaining));
                colors.add(remaining <= 60_000L ? YELLOW : WHITE);
            }
        }

        int x = Config.data.hudX;
        int y = Config.data.hudY;
        int lineHeight = 10;
        int width = 0;
        for (String l : lines) width = Math.max(width, mc.font.width(l));

        graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size() * lineHeight + 1, 0x90000000);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(mc.font, lines.get(i), x, y + i * lineHeight, colors.get(i), true);
        }
    }
}
