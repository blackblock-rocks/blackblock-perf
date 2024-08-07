package rocks.blackblock.perf.commands;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import rocks.blackblock.bib.command.CommandCreator;
import rocks.blackblock.bib.command.CommandLeaf;
import rocks.blackblock.bib.util.BibText;

@ApiStatus.Internal
public class PerfCommands {

    public static void init() {
        CommandLeaf blackblock = CommandCreator.getBlackblockRoot();
        CommandLeaf perf = blackblock.getChild("perf");
        registerSpawnCommand(perf);
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
}
