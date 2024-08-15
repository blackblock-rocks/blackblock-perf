package rocks.blackblock.perf.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.command.CommandCreator;
import rocks.blackblock.bib.command.CommandLeaf;
import rocks.blackblock.bib.debug.rendering.RenderLayer;
import rocks.blackblock.bib.debug.rendering.shapes.BoxShape;
import rocks.blackblock.bib.debug.rendering.shapes.payload.DebugShapesPayload;
import rocks.blackblock.bib.player.PlayerActivityInfo;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibPerf;
import rocks.blackblock.bib.util.BibServer;
import rocks.blackblock.bib.util.BibText;
import rocks.blackblock.perf.activation_range.ActivationRange;
import rocks.blackblock.perf.activation_range.EntityCluster;
import rocks.blackblock.perf.thread.DynamicThreads;

import java.util.List;

@ApiStatus.Internal
public class PerfCommands {

    public static void init() {
        CommandLeaf blackblock = CommandCreator.getBlackblockRoot();
        CommandLeaf perf = blackblock.getChild("perf");
        perf.onExecute(PerfCommands::onExecutePerfInfo);
        registerEntitiesCommands(perf);
        registerPlayerCommands(perf);
        registerAfkCommands(perf);
    }

    /**
     * Show perf info
     * @since 0.1.0
     */
    private static int onExecutePerfInfo(CommandContext<ServerCommandSource> context) {

        var source = context.getSource();
        var player = source.getPlayer();
        ServerWorld source_world = null;

        if (player != null) {
            source_world = player.getServerWorld();
        }

        BibText.Lore lore = BibText.createLore();

        lore.addLine(Text.literal("⚡ Performance Info ⚡").setStyle(Style.EMPTY.withBold(true).withUnderline(true)));

        if (source_world != null) {
            lore.addLine(Text.literal("World: ").append(Text.literal(source_world.getRegistryKey().getValue().getPath()).formatted(Formatting.AQUA)));
        }

        if (DynamicThreads.THREADS_ENABLED) {
            lore.addLine("Threadcount", DynamicThreads.THREADS_COUNT);

            for (var world : BibServer.getServer().getWorlds()) {

                if (source_world == null) {
                    lore.addLine("");
                    lore.addLine(Text.literal("World: ").append(Text.literal(world.getRegistryKey().getValue().getPath()).formatted(Formatting.AQUA)));
                } else if (source_world != world) {
                    continue;
                }

                BibPerf.Info info = world.bb$getPerformanceInfo();

                int loaded_chunks = world.getChunkManager().getLoadedChunkCount();
                int active_chunks = world.getChunkManager().chunkLoadingManager.bb$tickableChunkMap().size();

                MutableText loaded_chunk_text = Text.literal(loaded_chunks + "")
                        .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Loaded chunks"))))
                        .formatted(Formatting.AQUA);

                MutableText active_chunk_text = Text.literal(active_chunks + "")
                        .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Active chunks"))))
                        .formatted(Formatting.AQUA);

                lore.addLine("Chunks", loaded_chunk_text.append(Text.literal(" / ").formatted(Formatting.GRAY)).append(active_chunk_text));
                lore.addLine("MSPT", info.getMsptText());
                lore.addLine("TPS", info.getTpsText());

                int player_count = world.getPlayers().size();
                long afk_count = world.getPlayers().stream().filter(PlayerActivityInfo::bb$isAfk).count();

                lore.addLine("Players", Text.literal(player_count + "")
                    .formatted(Formatting.AQUA)
                    .append(Text.literal(" (").formatted(Formatting.GRAY))
                    .append(Text.literal(afk_count + " AFK").formatted(Formatting.GRAY))
                    .append(Text.literal(")").formatted(Formatting.GRAY))
                );

                MutableText distance_text = Text.literal(world.bb$getSimulationDistance() + "").formatted(Formatting.AQUA)
                        .append(Text.literal(" / ").formatted(Formatting.GRAY))
                        .append(Text.literal(world.bb$getMaxViewDistance() + "").formatted(Formatting.AQUA));

                lore.addLine("State", info.getStateText());

                lore.addLine("Distance", distance_text);
            }
        } else {
            lore.addLine("Threading not enabled!");
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
                ((PlayerActivityInfo) player).bb$setIsStationary(true);
                source.sendFeedback(() -> Text.literal("Marked player ").append(Text.literal(player.getNameForScoreboard()).formatted(Formatting.AQUA)).append(" as stationary"), true);
            });

            return 1;
        });

        CommandLeaf list_afk_leaf = afk.getChild("list-afk-players");

        list_afk_leaf.onExecute(context -> {

            var source = context.getSource();

            BibServer.getServer().getPlayerManager().getPlayerList().forEach(player -> {

                int ticks_since_last_movement = ((PlayerActivityInfo) player).bb$getTicksSinceLastMovement();
                BibLog.log("Ticks since last movement:", player, ticks_since_last_movement);

                if (player.bb$isAfk()) {
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

        CommandLeaf debug_renderer_leaf = entities_leaf.getChild("debug-renderer");
        debug_renderer_leaf.onExecute(PerfCommands::performDebugRenderer);
    }

    private static int performDebugRenderer(CommandContext<ServerCommandSource> context) {

        var source = context.getSource();
        var player = source.getPlayer();

        if (player == null) {
            return 0;
        }

        var world = player.getServerWorld();
        var groups = world.bb$getEntityClusters();

        if (groups == null || groups.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No entity groups found"), false);
            return 1;
        }

        DebugShapesPayload.clearAllShapes(player);

        int i = 0;
        for (EntityCluster group : groups) {

            i++;
            Box box = group.getSmallMergeBox();
            BoxShape debug_box = new BoxShape(box.getMinPos(), box.getMaxPos(), 0x33FF0055, RenderLayer.MIXED, 0xFF00FF00, RenderLayer.MIXED, 4f);

            Identifier shapeId = Identifier.of("blackblock", "debug_box_" + i);

            DebugShapesPayload.sendToPlayer(source.getPlayer(), List.of(new DebugShapesPayload.Set(shapeId, debug_box)));
        }

        return 1;
    }

    private record DebugPayload(PacketByteBuf buf) implements CustomPayload {
        public static final CustomPayload.Id<DebugPayload> ID = new CustomPayload.Id<>(Identifier.of("debug", "shapes"));
        //public static final PacketCodec<RegistryByteBuf, DebugPayload> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, DebugPayload::buf, DebugPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static void registerPlayerCommands(CommandLeaf perf) {

        CommandLeaf players_leaf = perf.getChild("players");
        CommandLeaf player_distances_leaf = players_leaf.getChild("distances");

        player_distances_leaf.onExecute(context -> {

            var source = context.getSource();

            BibText.Lore lore = BibText.createLore();
            lore.add("Player view distances: World / Client / Personal");

            for (var player : source.getServer().getPlayerManager().getPlayerList()) {

                MutableText distances = Text.literal(player.bb$getWorldViewDistance() + "")
                        .formatted(Formatting.AQUA)
                        .append(Text.literal(" / ").formatted(Formatting.GRAY))
                        .append(Text.literal(player.bb$getClientSideViewDistance() + "").formatted(Formatting.AQUA))
                        .append(Text.literal(" / ").formatted(Formatting.GRAY))
                        .append(Text.literal(player.bb$getPersonalViewDistance() + "").formatted(Formatting.AQUA));

                lore.addLine(player.getNameForScoreboard(), distances);
            }

            source.sendFeedback(lore, false);

            return 1;
        });
    }
}
