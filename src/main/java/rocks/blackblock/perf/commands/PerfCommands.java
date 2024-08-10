package rocks.blackblock.perf.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.command.CommandCreator;
import rocks.blackblock.bib.command.CommandLeaf;
import rocks.blackblock.bib.player.BlackblockPlayer;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.bib.util.BibServer;
import rocks.blackblock.bib.util.BibText;
import rocks.blackblock.perf.activation_range.ActivationRange;
import rocks.blackblock.perf.thread.DynamicThreads;
import rocks.blackblock.perf.thread.HasPerformanceInfo;

@ApiStatus.Internal
public class PerfCommands {

    public static void init() {
        CommandLeaf blackblock = CommandCreator.getBlackblockRoot();
        CommandLeaf perf = blackblock.getChild("perf");
        perf.onExecute(PerfCommands::onExecutePerfInfo);
        registerEntitiesCommands(perf);
        registerFakePlayerCommand(perf);
        registerAfkCommands(perf);
    }

    /**
     * Show perf info
     * @since 0.1.0
     */
    private static int onExecutePerfInfo(CommandContext<ServerCommandSource> context) {

        var source = context.getSource();

        BibText.Lore lore = BibText.createLore();
        lore.addLine("Threading", DynamicThreads.THREADS_ENABLED ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED));

        if (DynamicThreads.THREADS_ENABLED) {
            lore.addLine("Threadcount", DynamicThreads.THREADS_COUNT);
            lore.addLine("World: Loaded chunks / MSPT / TPS / Load / State");

            for (var world : BibServer.getServer().getWorlds()) {
                BibPerf.Info info = ((HasPerformanceInfo) world).bb$getPerformanceInfo();
                Text world_name = Text.literal(world.getRegistryKey().getValue().getPath());
                MutableText line = info.toTextLine();
                lore.addLine(world_name.copy().append(": ").append(line));
            }
        }

        source.sendFeedback(lore, false);

        return 1;
    }

    /**
     * Some AFK debug commands
     * @since 0.1.0
     */
    private static void registerAfkCommands(CommandLeaf perf) {

        CommandLeaf afk = perf.getChild("afk");
        CommandLeaf trigger = afk.getChild("make-all-players-afk");

        trigger.onExecute(context -> {

            var source = context.getSource();

            BibServer.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                ((BlackblockPlayer) player).bb$setIsStationary(true);
                source.sendFeedback(() -> Text.literal("Marked player ").append(Text.literal(player.getNameForScoreboard()).formatted(Formatting.AQUA)).append(" as stationary"), true);
            });

            return 1;
        });

        CommandLeaf list_afk_leaf = afk.getChild("list-afk-players");

        list_afk_leaf.onExecute(context -> {

            var source = context.getSource();

            BibServer.getServer().getPlayerManager().getPlayerList().forEach(player -> {

                int ticks_since_last_movement = ((BlackblockPlayer) player).bb$getTicksSinceLastMovement();
                BibLog.log("Ticks since last movement:", player, ticks_since_last_movement);

                if (((BlackblockPlayer) player).bb$isAfk()) {
                    source.sendFeedback(() -> Text.literal("Player ").append(Text.literal(player.getNameForScoreboard()).formatted(Formatting.AQUA)).append(" is AFK"), false);
                }
            });

            return 1;
        });
    }

    /**
     * Some Entity debug commands
     * @since 0.1.0
     */
    private static void registerEntitiesCommands(CommandLeaf perf) {

        CommandLeaf entities_leaf = perf.getChild("entities");
        CommandLeaf spawns_leaf = entities_leaf.getChild("spawns");

        spawns_leaf.onExecute(context -> {

            var player = context.getSource().getPlayer();

            if (player == null) {
                return 0;
            }

            World world = player.getServerWorld();
            var chunk_pos = player.getChunkPos();

            BibText.Lore lore = BibText.createLore();
            lore.add("Spawn groups in world " + world.getRegistryKey().getValue() + ":");

            for (SpawnGroup group : SpawnGroup.values()) {
                int original_capacity = group.bb$getOriginalCapacity();
                int capacity = group.bb$getCapacity(world);
                lore.addLine(group.getName(), capacity + " (original: " + original_capacity + ")");
            }

            var source = context.getSource();
            source.sendFeedback(lore, false);

            return 1;
        });

        CommandLeaf get_inactive_entities_leaf = entities_leaf.getChild("get-inactive-entities");

        get_inactive_entities_leaf.onExecute(context -> {

            var source = context.getSource();
            var player = source.getPlayer();

            if (player == null) {
                return 0;
            }

            World world = player.getServerWorld();

            BibText.Lore lore = BibText.createLore();
            lore.add("Inactive entities in world " + world.getRegistryKey().getValue() + " around you (100 block radius):");

            for (Entity entity : world.getOtherEntities(player, player.getBoundingBox().expand(100, 100, 100))) {
                if (entity.bb$isInactive()) {

                    var pos = entity.getPos();
                    var pos_str = Math.floor(pos.getX()) + ", " + Math.floor(pos.getY()) + ", " + Math.floor(pos.getZ());
                    var teleport_str = "/tp " + pos.getX() + " " + pos.getY() + " " + pos.getZ();

                    Text texts = Text.literal(pos_str)
                            .getWithStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleport_str))).get(0)
                            .copy()
                            .formatted(Formatting.GOLD)
                            .append(Text.literal(" - Tickcount: " + entity.bb$getPotentialTickCount()));

                    lore.addLine(entity.getType().getTranslationKey(), texts);
                }
            }

            source.sendFeedback(lore, false);
            return 1;
        });

        CommandLeaf show_activation_ranges_leaf = entities_leaf.getChild("show-activation-ranges");

        show_activation_ranges_leaf.onExecute(context -> {

            var source = context.getSource();
            var player = source.getPlayer();

            if (player == null) {
                return 0;
            }

            World world = player.getServerWorld();

            BibText.Lore lore = BibText.createLore();
            lore.add("Activation Range data in world " + world.getRegistryKey().getValue() + ":");
            lore.addLine("Name: Activation range / Active tick delay / Inactive wakeup interval");

            for (var range : ActivationRange.ACTIVATION_RANGES) {

                Text line = Text.literal(range.getName())
                        .append(": ")
                        .append(range.getActivationRangeText(world))
                        .append(" / ")
                        .append(range.getActiveTickDelayText(world))
                        .append(" / ")
                        .append(Text.literal(range.getInactiveWakeupInterval() + "").formatted(Formatting.AQUA));

                lore.addLine(line);
            }

            source.sendFeedback(lore, false);
            return 1;
        });
    }

    private static void registerFakePlayerCommand(CommandLeaf perf) {

        CommandLeaf fake_player_leaf = perf.getChild("fake_player");
        CommandLeaf spawn = fake_player_leaf.getChild("spawn");
        CommandLeaf names_leaf = spawn.getChild("names");
        names_leaf.setType(StringArgumentType.greedyString());

        names_leaf.onExecute(context -> {

            var source = context.getSource();

            String all_names = StringArgumentType.getString(context, "names");

            for (String user_name : all_names.split(" ")) {

                SkullBlockEntity.fetchProfileByName(user_name).thenAccept(optional_profile -> {

                    if (optional_profile.isEmpty()) {
                        return;
                    }

                    FakePlayer fake_player = FakePlayer.get(source.getWorld(), optional_profile.get());
                    BibLog.log("Created fake player:", fake_player);

                });
            }

            return 1;
        });
    }
}
