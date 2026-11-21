package dev.kotandsraka.visuals;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class KotCodeSounds {
    private static final Identifier HIT_UWU_ID =
        Identifier.fromNamespaceAndPath("kotandsraka_visuals", "hit_uwu_custom");

    public static SoundEvent HIT_UWU;

    private KotCodeSounds() {}

    public static void register() {
        HIT_UWU = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            HIT_UWU_ID,
            SoundEvent.createVariableRangeEvent(HIT_UWU_ID)
        );
    }
}
