package ua.zentix.incoming.strike;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadoutTest {
    @Test
    void binocularCountsCycleInBothDirectionsAndHandleCustomCounts() {
        for (WeaponType weapon : new WeaponType[]{WeaponType.DRONE, WeaponType.LOITER, WeaponType.MISSILE, WeaponType.ROCKET}) {
            Loadout l = Loadout.DEFAULT.withWeapon(weapon).withCount(1);
            for (int count : new int[]{3, 6, 12, 1}) {
                l = l.cycleCount(true);
                assertEquals(count, l.count(), weapon.name());
            }
            for (int count : new int[]{12, 6, 3, 1}) {
                l = l.cycleCount(false);
                assertEquals(count, l.count(), weapon.name());
            }
            assertEquals(6, l.withCount(4).cycleCount(true).count());
            assertEquals(3, l.withCount(4).cycleCount(false).count());
        }
    }

    @Test
    void gradCountDoesNotCarryOverAndBomberOrdersHaveOneBomb() {
        Loadout grad = Loadout.DEFAULT.withWeapon(WeaponType.ROCKET);
        assertEquals(12, grad.count());
        for (WeaponType weapon : new WeaponType[]{WeaponType.DRONE, WeaponType.LOITER, WeaponType.MISSILE, WeaponType.BUNKER}) {
            assertEquals(1, grad.withWeapon(weapon).count(), weapon.name());
            assertEquals(0, grad.withWeapon(weapon).spread(), weapon.name());
        }
        Loadout bomber = grad.withWeapon(WeaponType.BUNKER).withCount(100);
        assertEquals(1, bomber.count());
        assertEquals(1, bomber.cycleCount(true).count());
        assertEquals(1, bomber.cycleCount(false).count());
        assertEquals(1, Loadout.DEFAULT.withWeapon(WeaponType.NUKE).cycleCount(true).count());
        assertEquals(1, Loadout.DEFAULT.withNuke(new Loadout.Nuke(1, false, true)).cycleCount(true).count());
        CompoundTag saved = new CompoundTag();
        saved.putString("weapon", "bunker");
        saved.putInt("count", 12);
        assertEquals(1, Loadout.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().count());
    }

    @Test
    void newAndMissingDesignatorSettingsUseGroundBursts() {
        assertFalse(Loadout.Nuke.DEFAULT.airBurst());
        assertFalse(Loadout.DEFAULT.withWeapon(WeaponType.NUKE).nuke().airBurst());
        assertFalse(Loadout.Nuke.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow().airBurst());
        assertFalse(Loadout.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow().nuke().airBurst());
        assertFalse(new Loadout(WeaponType.NUKE, 1, 0, TargetMode.LOOK, "", null).nuke().airBurst());
    }

    @Test
    void explicitSavedAirRemainsAvailableAndBothModesRoundTrip() {
        CompoundTag savedAir = new CompoundTag();
        savedAir.putBoolean("air_burst", true);
        Loadout.Nuke air = Loadout.Nuke.CODEC.parse(NbtOps.INSTANCE, savedAir).getOrThrow();
        assertTrue(air.airBurst());
        for (boolean burst : new boolean[]{false, true}) {
            Loadout.Nuke nuke = new Loadout.Nuke(6, burst, true);
            var encoded = Loadout.Nuke.CODEC.encodeStart(NbtOps.INSTANCE, nuke).getOrThrow();
            assertEquals(nuke, Loadout.Nuke.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
        }
    }
}
