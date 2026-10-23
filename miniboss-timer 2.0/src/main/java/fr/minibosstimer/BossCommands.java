package fr.minibosstimer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.Map;

/** Commandes client : /mbt ... (ne sont jamais envoyées au serveur). */
public final class BossCommands {
    private BossCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("mbt")
                        .executes(ctx -> help(ctx.getSource()))
                        .then(ClientCommands.literal("list")
                                .executes(ctx -> list(ctx.getSource())))
                        .then(ClientCommands.literal("clear")
                                .executes(ctx -> {
                                    TimerManager.clear();
                                    ok(ctx.getSource(), "Tous les timers ont ete supprimes.");
                                    return 1;
                                }))
                        .then(ClientCommands.literal("start")
                                .then(ClientCommands.argument("nom", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = Config.cleanName(StringArgumentType.getString(ctx, "nom"));
                                            KillDetector.handleKill(name, true);
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("add")
                                .then(ClientCommands.argument("nom", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = Config.cleanName(StringArgumentType.getString(ctx, "nom"));
                                            Config.data.bosses.put(name, 0);
                                            Config.save();
                                            ok(ctx.getSource(), "Mini-boss ajoute : " + name);
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("remove")
                                .then(ClientCommands.argument("nom", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = Config.cleanName(StringArgumentType.getString(ctx, "nom"));
                                            boolean removed = Config.data.bosses.keySet()
                                                    .removeIf(k -> k.equalsIgnoreCase(name));
                                            Config.save();
                                            ok(ctx.getSource(), removed ? "Retire : " + name : "Introuvable : " + name);
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("bosses")
                                .executes(ctx -> bosses(ctx.getSource())))
                        .then(ClientCommands.literal("minutes")
                                .then(ClientCommands.argument("n", IntegerArgumentType.integer(1, 600))
                                        .executes(ctx -> {
                                            Config.data.defaultRespawnMinutes = IntegerArgumentType.getInteger(ctx, "n");
                                            Config.save();
                                            ok(ctx.getSource(), "Respawn par defaut : "
                                                    + Config.data.defaultRespawnMinutes + " min");
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("server")
                                .executes(ctx -> {
                                    ok(ctx.getSource(), "Serveur actuel : " + ServerResolver.current()
                                            + (ServerResolver.isDetected() ? " (detecte via /voyager)" : "")
                                            + " - /mbt server <nom> pour forcer, /mbt server auto pour tout remettre en automatique");
                                    return 1;
                                })
                                .then(ClientCommands.argument("nom", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "nom").trim();
                                            if (name.equalsIgnoreCase("auto")) name = "";
                                            Config.data.manualServerName = name;
                                            ServerResolver.detected = "";
                                            Config.save();
                                            ok(ctx.getSource(), "Serveur : " + ServerResolver.current());
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("hud")
                                .executes(ctx -> {
                                    Config.data.showHud = !Config.data.showHud;
                                    Config.save();
                                    ok(ctx.getSource(), "Affichage HUD : " + (Config.data.showHud ? "active" : "desactive"));
                                    return 1;
                                }))
                        .then(ClientCommands.literal("debug")
                                .executes(ctx -> {
                                    Config.data.debug = !Config.data.debug;
                                    Config.save();
                                    ok(ctx.getSource(), "Debug : " + (Config.data.debug ? "active" : "desactive")
                                            + " (affiche le nom des entites mortes pour les ajouter avec /mbt add)");
                                    return 1;
                                }))
                        .then(ClientCommands.literal("reload")
                                .executes(ctx -> {
                                    Config.load();
                                    ok(ctx.getSource(), "Config rechargee.");
                                    return 1;
                                }))
                ));
    }

    private static void ok(FabricClientCommandSource s, String text) {
        s.sendFeedback(Msg.prefix().append(Msg.t(text, ChatFormatting.GRAY)));
    }

    private static int help(FabricClientCommandSource s) {
        s.sendFeedback(Msg.t("=== MiniBoss Timer ===", ChatFormatting.GOLD));
        String[] lines = {
                "/mbt list - timers en cours",
                "/mbt start <boss> - lancer un timer a la main",
                "/mbt add <boss> | remove <boss> | bosses - gerer la liste des mini-boss",
                "/mbt minutes <n> - temps de respawn par defaut",
                "/mbt server [nom|auto] - voir / forcer le nom du serveur (auto apres /voyager)",
                "/mbt hud - afficher/masquer le panneau",
                "/mbt debug - affiche les noms des entites mortes",
                "/mbt clear - supprimer tous les timers",
                "/mbt reload - recharger config/miniboss_timer.json"
        };
        for (String l : lines) s.sendFeedback(Msg.t(l, ChatFormatting.GRAY));
        return 1;
    }

    private static int list(FabricClientCommandSource s) {
        List<TimerManager.Timer> timers = TimerManager.snapshot();
        if (timers.isEmpty()) {
            ok(s, "Aucun timer en cours.");
            return 1;
        }
        long now = System.currentTimeMillis();
        s.sendFeedback(Msg.t("=== Timers de respawn ===", ChatFormatting.GOLD));
        for (TimerManager.Timer t : timers) {
            long remaining = t.respawnAt - now;
            s.sendFeedback(Msg.t("[" + t.server + "] ", ChatFormatting.AQUA)
                    .append(Msg.t(t.boss + " ", ChatFormatting.YELLOW))
                    .append(remaining <= 0
                            ? Msg.t("PRET !", ChatFormatting.GREEN)
                            : Msg.t(Msg.duration(remaining) + " (a " + Msg.clock(t.respawnAt) + ")", ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int bosses(FabricClientCommandSource s) {
        s.sendFeedback(Msg.t("=== Mini-boss connus ===", ChatFormatting.GOLD));
        if (Config.data.bosses.isEmpty()) {
            s.sendFeedback(Msg.t("(liste vide)", ChatFormatting.GRAY));
        }
        for (Map.Entry<String, Integer> e : Config.data.bosses.entrySet()) {
            int min = (e.getValue() != null && e.getValue() > 0) ? e.getValue() : Config.data.defaultRespawnMinutes;
            s.sendFeedback(Msg.t("- " + e.getKey() + " : " + min + " min", ChatFormatting.GRAY));
        }
        s.sendFeedback(Msg.t("Detection auto par PV : "
                + (Config.data.useHealthHeuristic ? "oui (nom perso + " + Config.data.minBossMaxHealth + " PV mini)" : "non"),
                ChatFormatting.DARK_GRAY));
        return 1;
    }
}
