package dev.xmastab.mixin;

import dev.xmastab.XmasTabRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Podmienia rysowanie listy graczy (Tab) na świąteczną wersję.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    /** Nazwa gracza z uwzględnieniem drużyny / display name - tak jak w vanilli. */
    @Shadow
    public abstract Component getNameForDisplay(PlayerInfo info);

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void xmastab$render(GuiGraphicsExtractor graphics, int screenWidth,
                                Scoreboard scoreboard, @Nullable Objective objective,
                                CallbackInfo ci) {
        XmasTabRenderer.render(graphics, screenWidth, this::getNameForDisplay);
        ci.cancel();
    }
}
