package rocks.blackblock.perf.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.BibMod;
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
import rocks.blackblock.perf.distance.AreaPlayerChunkWatchingManager;
import rocks.blackblock.perf.thread.DynamicThreads;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApiStatus.Internal
public class PerfCommands {

    public static void init() {
        CommandLeaf blackblock = CommandCreator.getBlackblockRoot();
        CommandLeaf perf = blackblock.getChild("perf");
        perf.onExecute(PerfCommands::onExecutePerfInfo);
        registerEntitiesCommands(perf);
        registerPlayerCommands(perf);
        registerAfkCommands(perf);
        registerChunkWatchCommands(perf);
        registerFlySpeedCommand(perf);

        if (!BibMod.PLATFORM.isModLoaded("blackblock-core")) {
            registerTweakCommands(blackblock);
        }
    }

    private static void registerFlySpeedCommand(CommandLeaf perf) {
        CommandLeaf flySpeed = perf.getChild("fly-speed");

        flySpeed.onExecute(context -> {

            var source = context.getSource();
            var player = source.getPlayer();

            float j = player.getAbilities().getFlySpeed();

            BibText.Lore lore = BibText.createLore();
            lore.addLine(Text.literal("Fly speed: ").formatted(Formatting.GRAY)
                    .append(Text.literal((j * 100) + "").formatted(Formatting.GOLD)));

            source.sendFeedback(lore, false);

            return 1;
        });

        CommandLeaf value = flySpeed.getChild("new-value");
        value.setType(IntegerArgumentType.integer());

        value.onExecute(context -> {

            float new_value = (float) IntegerArgumentType.getInteger(context, "new-value") / 100;
            var source = context.getSource();
            var player = source.getPlayer();

            float l = MathHelper.clamp(new_value, 0.0F, 1F);
            player.getAbilities().setFlySpeed(l);
            player.sendAbilitiesUpdate();

            BibText.Lore lore = BibText.createLore();
            lore.addLine(Text.literal("Your fly speed is now: ").formatted(Formatting.GRAY)
                    .append(Text.literal((l * 100) + "").formatted(Formatting.GOLD)));

            source.sendFeedback(lore, false);

            return 1;
        });
    }

    private static void registerChunkWatchCommands(CommandLeaf perf) {
        CommandLeaf chunkWatch = perf.getChild("chunk_watch");
        chunkWatch.onExecute(PerfCommands::onExecuteChunkWatchInfo);
    }

    private static int onExecuteChunkWatchInfo(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var player = source.getPlayer();
        ServerWorld source_world = player.getServerWorld();

        ServerChunkManager chunkManager = source_world.getChunkManager();
        ServerChunkLoadingManager loadingManager = chunkManager.chunkLoadingManager;
        AreaPlayerChunkWatchingManager areaManager = loadingManager.bb$getAreaPlayerChunkWatchingManager();

        BibText.Lore lore = BibText.createLore();
        lore.addLine(Text.literal("Area Player Chunk Watching Manager:"));
        lore.addLine(Text.literal(" - Watch distance: " + areaManager.getWatchDistance()));

        Set<ServerPlayerEntity> players = areaManager.getAllPlayers();
        lore.addLine(Text.literal(" - Watching " + players.size() + " players:"));

        for (ServerPlayerEntity wplayer : players) {
            lore.addLine(Text.literal(" -- " + wplayer.getNameForScoreboard()));

            int view_distance = areaManager.getViewDistance(wplayer);
            lore.addLine(Text.literal(" --- View distance: " + view_distance));

            ChunkPos chunkPos = areaManager.getPlayerChunkPosition(wplayer);

            if (chunkPos == null) {
                lore.addLine(" --- Chunk pos: null!");
            } else {
                lore.addLine(" --- Chunk pos: " + chunkPos.x + ", " + chunkPos.z);
            }

            var list = loadingManager.getPlayersWatchingChunk(chunkPos, true);

            if (list.contains(wplayer)) {
                lore.addLine(" --- On edge: true");
            } else {
                lore.addLine(" --- On edge: false");
            }

            //lore.addLine(" --- Is on track edge: " + loadingManager)

            var wpos = wplayer.getWatchedSection().toChunkPos();
            lore.addLine(" --- Watched section: " + wpos.x + ", " + wpos.z);

            lore.addLine(" ");
        }

        lore.addLine(" ");

        ChunkPos currentChunkPos = player.getChunkPos();
        lore.addLine("You are in chunk " + currentChunkPos.x + ", " + currentChunkPos.z);
        var playersWatching = loadingManager.getPlayersWatchingChunk(currentChunkPos);
        lore.addLine("  -- There are " + playersWatching.size() + " players watching this chunk");

        source.sendFeedback(lore, false);

        return 1;
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

        CommandLeaf box_leaf = debug_renderer_leaf.getChild("box");
        CommandLeaf max_x = box_leaf.getChild("max-x");
        max_x.setType(IntegerArgumentType.integer());
        CommandLeaf max_y = max_x.getChild("max-y");
        max_y.setType(IntegerArgumentType.integer());
        CommandLeaf max_z = max_y.getChild("max-z");
        max_z.setType(IntegerArgumentType.integer());

        CommandLeaf min_x = max_z.getChild("min-x");
        min_x.setType(IntegerArgumentType.integer());
        CommandLeaf min_y = min_x.getChild("min-y");
        min_y.setType(IntegerArgumentType.integer());
        CommandLeaf min_z = min_y.getChild("min-z");
        min_z.setType(IntegerArgumentType.integer());

        min_z.onExecute(PerfCommands::performCustomBoxDebugRender);
    }

    private static int performCustomBoxDebugRender(CommandContext<ServerCommandSource> context) {

        var source = context.getSource();
        var player = source.getPlayer();

        if (player == null) {
            return 0;
        }

        int max_x = IntegerArgumentType.getInteger(context, "max-x");
        int max_y = IntegerArgumentType.getInteger(context, "max-y");
        int max_z = IntegerArgumentType.getInteger(context, "max-z");
        int min_x = IntegerArgumentType.getInteger(context, "min-x");
        int min_y = IntegerArgumentType.getInteger(context, "min-y");
        int min_z = IntegerArgumentType.getInteger(context, "min-z");

        Box box = new Box(min_x, min_y, min_z, max_x, max_y, max_z);
        BoxShape debug_box = new BoxShape(box.getMinPos(), box.getMaxPos(), 0x33FF0055, RenderLayer.MIXED, 0x8000FF30, RenderLayer.MIXED, 8f);

        Identifier shapeId = Identifier.of("blackblock", "player_debug_box");
        DebugShapesPayload.sendToPlayer(player, List.of(new DebugShapesPayload.Set(shapeId, debug_box)));
        return 1;
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
        Set<EntityCluster> super_clusters = new HashSet<>();

        int i = 0;
        for (EntityCluster group : groups) {

            i++;
            Box box = group.getSmallMergeBox();
            BoxShape debug_box = new BoxShape(box.getMinPos(), box.getMaxPos(), 0x33FF0055, RenderLayer.MIXED, 0xFF00FF00, RenderLayer.MIXED, 4f);

            Identifier shapeId = Identifier.of("blackblock", "debug_box_" + i);

            DebugShapesPayload.sendToPlayer(source.getPlayer(), List.of(new DebugShapesPayload.Set(shapeId, debug_box)));

            EntityCluster super_cluster = group.getSuperCluster();
            if (super_cluster != null) {
                super_clusters.add(super_cluster);
            }
        }

        for (EntityCluster super_cluster : super_clusters) {
            Box box = super_cluster.getSmallMergeBox();
            BoxShape debug_box = new BoxShape(box.getMinPos(), box.getMaxPos(), 0x15FF0040, RenderLayer.MIXED, 0x5050FF00, RenderLayer.MIXED, 2f);

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

    /**
     * Register all the tweak commands
     *
     * @since    0.2.0
     */
    private static void registerTweakCommands(CommandLeaf blackblock) {

        // Get the "tweaks" leaf of the "blackblock" root
        CommandLeaf global_tweaks = blackblock.getChild("tweaks");
        BibMod.GLOBAL_TWEAKS.addToCommandLeaf(global_tweaks);

        // Get the "tweaks" root leaf, meant for players
        CommandLeaf player_tweaks = CommandCreator.getRoot("tweaks");
        BibMod.PLAYER_TWEAKS.addToCommandLeaf(player_tweaks);
    }
}
