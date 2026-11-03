package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;


/**
 * AimEr — richtet die Kamera automatisch auf das beste Ziel im Umkreis aus.
 *
 * <p>Bei mehreren moeglichen Zielen werden <b>Spieler immer vor Mobs</b>
 * priorisiert; erst bei gleicher Kategorie entscheidet die Naehe. Der Treffer
 * selbst (Schlagen) bleibt ausschliesslich manuell — dieser Cheat dreht nur die
 * Kamera.</p>
 */
public class AimEr extends Cheat {

    /** Auswahl des Zieltyps. */
    public static final String TARGET_PLAYERS = "Spieler";
    public static final String TARGET_MOBS = "Mobs";
    public static final String TARGET_BOTH = "Beide";

    private final Option target = choice("target", "Ziele", TARGET_BOTH,
            TARGET_PLAYERS, TARGET_MOBS, TARGET_BOTH);
    private final Option range = number("range", "Reichweite (m)", 5.0, 1.0, 12.0, 0.5);
    private final Option fov = number("fov", "Blickfeld (°)", 180.0, 30.0, 360.0, 5.0);
    private final Option speed = number("speed", "Drehgeschwindigkeit (%)", 45.0, 5.0, 100.0, 5.0);
    private final Option vertical = bool("vertical", "Auch vertikal zielen", true);
    private final Option onlyVisible = bool("onlyVisible", "Nur sichtbare Ziele", false);

    /** Aktuelles Ziel, damit die HUD-Anzeige es nutzen kann. */
    private Entity currentTarget;

    public AimEr() {
        super("aimer", "AimEr", "Richtet die Kamera auf Spieler/Mobs. Schlaege bleiben manuell.");
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
    }

    public Entity currentTarget() {
        return currentTarget;
    }

    @Override
    protected void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null || client.world == null) return;

        currentTarget = findTarget(client, player);
        if (currentTarget == null) return;

        Vec3d eye = player.getEyePos();
        // Auf die Augenmitte des Ziels zielen, damit auch kleine Ziele treffbar sind.
        Vec3d aim = currentTarget.getEyePos();
        double dx = aim.x - eye.x;
        double dy = aim.y - eye.y;
        double dz = aim.z - eye.z;

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI)));

        // Schrittweise drehen, damit die Bewegung nicht ruckartig wirkt.
        float factor = (float) (speed.asNumber() / 100.0);
        float yawStep = MathHelper.wrapDegrees(targetYaw - player.getYaw()) * factor;
        float pitchStep = MathHelper.wrapDegrees(targetPitch - player.getPitch()) * factor;

        player.setYaw(player.getYaw() + yawStep);
        if (vertical.asBoolean()) {
            float newPitch = MathHelper.clamp(player.getPitch() + pitchStep, -90.0F, 90.0F);
            player.setPitch(newPitch);
        }
    }

    /**
     * Sucht das beste Ziel. Spieler haben grundsaetzlich Vorrang vor Mobs; bei
     * mehreren Zielen derselben Kategorie gewinnt das naechste.
     */
    private Entity findTarget(MinecraftClient client, PlayerEntity player) {
        String mode = target.asChoice();
        double maxDistance = range.asNumber();
        double maxDistanceSq = maxDistance * maxDistance;
        float maxAngle = (float) (fov.asNumber() / 2.0);

        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);

        Entity bestPlayer = null;
        double bestPlayerDist = Double.MAX_VALUE;
        Entity bestMob = null;
        double bestMobDist = Double.MAX_VALUE;

        for (Entity entity : client.world.getEntities()) {
            if (entity == player || entity == client.getCameraEntity()) continue;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

            boolean isPlayer = entity instanceof PlayerEntity;
            // Mobs = alle lebenden Nicht-Spieler ausser Ruestungsstaendern.
            boolean isMob = !isPlayer && !(entity instanceof ArmorStandEntity);
            if (isPlayer && !accepts(mode, TARGET_PLAYERS)) continue;
            if (isMob && !accepts(mode, TARGET_MOBS)) continue;
            if (!isPlayer && !isMob) continue;

            double distanceSq = eye.squaredDistanceTo(entity.getEyePos());
            if (distanceSq > maxDistanceSq) continue;

            if (!withinFov(eye, look, entity, maxAngle)) continue;
            if (onlyVisible.asBoolean() && !player.canSee(entity)) continue;

            if (isPlayer && distanceSq < bestPlayerDist) {
                bestPlayerDist = distanceSq;
                bestPlayer = entity;
            } else if (isMob && distanceSq < bestMobDist) {
                bestMobDist = distanceSq;
                bestMob = entity;
            }
        }

        return bestPlayer != null ? bestPlayer : bestMob;
    }

    private boolean accepts(String mode, String category) {
        return TARGET_BOTH.equals(mode) || mode.equals(category);
    }

    private boolean withinFov(Vec3d eye, Vec3d look, Entity entity, float maxAngle) {
        if (maxAngle >= 180.0F) return true;
        Vec3d toTarget = entity.getBoundingBox().getCenter().subtract(eye).normalize();
        double angle = Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(toTarget), -1.0, 1.0)));
        return angle <= maxAngle;
    }
}
