package de.consolecrafter.ccraft.cheats.impl;

import de.consolecrafter.ccraft.cheats.Cheat;
import de.consolecrafter.ccraft.cheats.Option;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * ChestEsp — markiert Behaelter in der Naehe mit einem 2D-Kasten.
 *
 * <p>Da der Client alle geladenen BlockEntities kennt, laesst sich die Position
 * der Behaelter direkt aus der Welt lesen; eine Reichweitenbegrenzung haelt die
 * Anzeige uebersichtlich.</p>
 */
public class ChestEsp extends Cheat {

    private final Option color = number("color", "Farbe (RGB)", 16755200, 0, 16777215, 1);
    private final Option maxDistance = number("maxDistance", "Max. Entfernung (m)", 32.0, 4.0, 128.0, 4.0);
    private final Option showLabel = bool("label", "Behaelterart anzeigen", true);
    private final Option enderChests = bool("enderChests", "Endertruhen anzeigen", true);

    public ChestEsp() {
        super("chestesp", "ChestESP", "Markiert Truhen und andere Behaelter in der Naehe.");
    }

    @Override
    protected void onHudRender(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int rgb = color.asInt();
        int border = EspRender.withAlpha(rgb, 0xFF);
        double max = maxDistance.asNumber();

        for (BlockEntity blockEntity : blockEntitiesNear(client, max)) {
            String label = label(blockEntity);
            if (label == null) continue;

            Box box = new Box(blockEntity.getPos()).expand(0.02);
            double[] rect = EspRender.projectBox(box);
            if (rect == null) continue;

            EspRender.drawBoxOutline(ctx, rect, border);
            if (showLabel.asBoolean()) {
                var renderer = client.textRenderer;
                int x = (int) Math.round((rect[0] + rect[2]) / 2.0 - renderer.getWidth(label) / 2.0);
                ctx.drawTextWithShadow(renderer, label, x, (int) Math.round(rect[1]) - 10, border);
            }
        }
    }

    /**
     * Alle Behaelter-BlockEntities im Umkreis von {@code max} Bloecken.
     *
     * <p>{@code ClientWorld.getBlockEntities()} ist hierfuer unbrauchbar: Die
     * Menge wird nur in {@code loadBlockEntity()} befuellt, und das verlangt
     * einen BlockEntityRenderer mit {@code rendersOutsideBoundingBox() == true}.
     * Truhen, Oefen und Co. erfuellen das nicht — die Menge ist daher praktisch
     * immer leer. Deshalb werden die geladenen Chunks im Umkreis direkt
     * durchsucht.</p>
     */
    private static List<BlockEntity> blockEntitiesNear(MinecraftClient client, double max) {
        List<BlockEntity> found = new ArrayList<>();
        BlockPos origin = client.player.getBlockPos();

        int chunkRadius = (int) Math.ceil(max / 16.0) + 1;
        int centerChunkX = origin.getX() >> 4;
        int centerChunkZ = origin.getZ() >> 4;

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                WorldChunk chunk = client.world.getChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity.getPos().getSquaredDistance(origin) <= max * max) {
                        found.add(blockEntity);
                    }
                }
            }
        }
        return found;
    }

    /**
     * Anzeigename des Behaelters, oder {@code null}, wenn es keiner ist.
     *
     * <p>{@code DropperBlockEntity} erbt von {@code DispenserBlockEntity}, muss
     * also VOR dem Spender geprueft werden — sonst wird jeder Werfer als
     * "Spender" beschriftet, weil {@code instanceof} auch auf die Oberklasse
     * zutrifft.</p>
     */
    private String label(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity) return "Truhe";
        if (blockEntity instanceof BarrelBlockEntity) return "Fass";
        if (blockEntity instanceof ShulkerBoxBlockEntity) return "Shulker";
        if (blockEntity instanceof FurnaceBlockEntity) return "Ofen";
        if (blockEntity instanceof HopperBlockEntity) return "Trichter";
        // Reihenfolge wichtig: Dropper ist eine Unterklasse von Dispenser.
        if (blockEntity instanceof DropperBlockEntity) return "Werfer";
        if (blockEntity instanceof DispenserBlockEntity) return "Spender";
        if (enderChests.asBoolean() && blockEntity instanceof EnderChestBlockEntity) return "Endertruhe";
        return null;
    }
}
