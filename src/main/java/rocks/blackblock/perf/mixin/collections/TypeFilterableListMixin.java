package rocks.blackblock.perf.mixin.collections;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.collection.TypeFilterableList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;

/**
 * Optimizes TypeFilterableList for improved performance.
 * Based upon VMP implementation
 *
 * This mixin enhances the TypeFilterableList class by:
 * - Using more efficient FastUtil data structures
 * - Optimizing the getAllOfType method
 * - Providing direct access to the backing array
 * - Ensuring compatibility with other performance mods like Lithium
 *
 * @author   ishland
 * @author   Jelle De Loecker <jelle@elevenways.be>
 * @since    0.1.0
 */
@Mixin(value = TypeFilterableList.class, priority = 1005)
public abstract class TypeFilterableListMixin<T> extends AbstractCollection<T> {

    @Mutable
    @Shadow
    @Final
    private Map<Class<?>, List<T>> elementsByType;

    @Mutable
    @Shadow
    @Final
    private List<T> allElements;

    @Shadow
    @Final
    private Class<T> elementType;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/util/collection/TypeFilterableList;elementsByType:Ljava/util/Map;", opcode = Opcodes.PUTFIELD))
    private void redirectSetElementsByType(TypeFilterableList<T> instance, Map<Class<?>, List<T>> value) {
        this.elementsByType = new Object2ObjectLinkedOpenHashMap<>();
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/util/collection/TypeFilterableList;allElements:Ljava/util/List;", opcode = Opcodes.PUTFIELD))
    private void redirectSetAllElements(TypeFilterableList<T> instance, List<T> value) {
        this.allElements = new ObjectArrayList<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false))
    private HashMap<?, ?> redirectNewHashMap() {
        return null; // avoid unnecessary alloc
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
    private ArrayList<?> redirectNewArrayList() {
        return null;
    }

    /**
     * @author ishland
     * @reason use fastutil array list for faster iteration & use array for filtering iteration
     */
    @Overwrite
    public <S> Collection<S> getAllOfType(Class<S> type) {
        List<T> cached = this.elementsByType.get(type);
        if (cached != null) return (Collection<S>) cached;

        if (!this.elementType.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Don't know how to search for " + type);
        } else {
            List<? extends T> list = this.elementsByType.computeIfAbsent(type,
                    typeClass -> {
                        ObjectArrayList<T> ts = new ObjectArrayList<>(this.allElements.size());
                        for (Object _allElement : ((ObjectArrayList<T>) this.allElements).elements()) {
                            if (typeClass.isInstance(_allElement)) {
                                ts.add((T) _allElement);
                            }
                        }
                        return ts;
                    }
            );
            return (Collection<S>) list;
        }
    }
}
