package rocks.blackblock.perf.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.command.CommandCreator;
import rocks.blackblock.bib.command.CommandLeaf;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibText;

@ApiStatus.Internal
public class PerfCommands {

    public static void init() {
        CommandLeaf blackblock = CommandCreator.getBlackblockRoot();
        CommandLeaf perf = blackblock.getChild("perf");
        registerSpawnCommand(perf);
        registerFakePlayerCommand(perf);
    }

    private static void registerSpawnCommand(CommandLeaf perf) {

        CommandLeaf spawns = perf.getChild("spawns");

        spawns.onExecute(context -> {

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
