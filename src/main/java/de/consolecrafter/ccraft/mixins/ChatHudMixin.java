package de.consolecrafter.ccraft.mixins;

import de.consolecrafter.ccraft.cheats.impl.NameProtect;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Schreibt den eigenen Namen in Chatzeilen um.
 *
 * <p>Angefasst wird die zentrale Ueberladung
 * {@code addMessage(Text, MessageSignatureData, MessageIndicator)}: der
 * Ein-Argument-Aufruf delegiert nur hierher, und die Netzwerk-Chat-Nachrichten
 * landen ueber {@code MessageHandler} ebenfalls hier. Am Kopf eingehangen wird
 * der Text umgebaut, bevor die Chatzeile entsteht — dadurch schuetzt die
 * Ersetzung auch die Chat-Historie und die Narration.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Text ccraft$protectChatName(Text message) {
        return NameProtect.protectChat(message);
    }
}
