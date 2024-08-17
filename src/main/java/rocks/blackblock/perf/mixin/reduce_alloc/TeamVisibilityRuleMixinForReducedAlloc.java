package rocks.blackblock.perf.mixin.reduce_alloc;

import net.minecraft.scoreboard.AbstractTeam;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rocks.blackblock.perf.dedup.Constants;

import java.util.Map;

/**
 * Don't create a new empty-string-array each time
 * the visibility rule enum is iterated over
 *
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(AbstractTeam.VisibilityRule.class)
public class TeamVisibilityRuleMixinForReducedAlloc {

    @Shadow
    @Final
    private static Map<String, AbstractTeam.VisibilityRule> VISIBILITY_RULES;

    /**
     * @author QPCrummer
     * @reason Reduce Allocations
     */
    @Overwrite
    public static String[] getKeys() {
        return VISIBILITY_RULES.keySet().toArray(Constants.emptyStringArray);
    }
}
