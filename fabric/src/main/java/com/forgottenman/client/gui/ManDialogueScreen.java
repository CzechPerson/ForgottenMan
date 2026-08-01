package com.forgottenman.client.gui;

import com.forgottenman.ForgottenMan;
import com.forgottenman.network.GiveEggPayload;
import com.forgottenman.network.ManDialogueFinishedPayload;
import com.forgottenman.network.RealityState;
import com.forgottenman.registry.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Deltarune-style narration box using the game's dialogue font and text box frame,
 */
public class ManDialogueScreen extends Screen {
    private static final ResourceLocation DIALOGUE_FONT = ForgottenMan.id("dialogue");
    private static final ResourceLocation FRAME = ForgottenMan.id("textures/gui/dialogue_frame.png");
    private static final Style FONT_STYLE = Style.EMPTY.withFont(DIALOGUE_FONT);
    private static final int LINE_COUNT = 14;
    private static final float CHARS_PER_TICK = 2.0F;
    // Frame texture: the assembled "Text box Ch1+2" asset, background made transparent.
    // 8px blue-purple outer band, 4px white line, 2px inner trim (14px border total),
    // sparkle motifs at the corners. Drawn 1:1 so it stays crisp at any gui scale.
    private static final int TEX_W = 593;
    private static final int TEX_H = 167;
    private static final int CORNER_SRC = 28;  // Corner sparkles end before x=30
    private static final int EDGE_SRC = 14;
    private static final int EDGE_AT = 30;     // Sparkle-free border stretch
    private static final int BORDER = 14;      // Full border band thickness
    private static final int SHADOW_COLOR = 0xFF130F50; // Sampled from the game text

    private final List<String> lines = new ArrayList<>();
    private int lineIndex;
    private float revealed;
    private boolean finished;
    private boolean hallExpanded;

    public ManDialogueScreen() {
        super(Component.empty());
        for (int i = 0; i < LINE_COUNT; i++) {
            lines.add(I18n.get("forgottenman.dialogue.man." + i));
        }
    }

    @Override
    public void tick() {
        String line = lines.get(lineIndex);
        if (revealed < line.length()) {
            float before = revealed;
            revealed = Math.min(line.length(), revealed + CHARS_PER_TICK);
            // One blip per typed chunk at fixed pitch, same as the game's snd_text
            if (hasVisibleChars(line, (int) before, (int) revealed)) {
                play(ModSounds.MAN_VOICE.get(), 1.0F, 1.0F);
            }
            checkMidpointBeat();
        }
    }

    // Line 6 shatters reality at its midpoint, mid-sentence
    private void checkMidpointBeat() {
        if (!hallExpanded && lineIndex == 6 && revealed >= lines.get(6).length() / 2.0F) {
            hallExpanded = true;
            RealityState.setMirrorLevel(2);
            play(ModSounds.REALITY_SHATTER.get(), 1.0F, 1.0F);
            com.forgottenman.client.ScreenShake.start(45, 3.2F); // The ground gives way
        }
    }

    private static boolean hasVisibleChars(String line, int from, int to) {
        for (int i = from; i < Math.min(to, line.length()); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean lineComplete() {
        return revealed >= lines.get(lineIndex).length();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_X) {
            if (!lineComplete()) {
                revealed = lines.get(lineIndex).length();
                checkMidpointBeat();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_Z || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (lineComplete()) {
                advance();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void advance() {
        // Story beats
        switch (lineIndex) {
            case 1 -> // "(He offers you something.)"
                    ClientPlayNetworking.send(GiveEggPayload.INSTANCE);
            case 3 -> { // "(He pointed into the distance.)"
                RealityState.setMirrorLevel(1);
                play(ModSounds.REALITY_CRACK.get(), 1.0F, 1.0F);
            }
            case 12 -> // "(Well, the man smiled.)" The copies go home, silently
                    RealityState.setMirrorLevel(0);
            default -> { }
        }
        lineIndex++;
        revealed = 0.0F;
        if (lineIndex >= lines.size()) {
            finished = true;
            ClientPlayNetworking.send(ManDialogueFinishedPayload.INSTANCE);
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }
    }

    private void play(SoundEvent sound, float pitch, float volume) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    @Override
    public void removed() {
        if (!finished) {
            // Dismissed early: put reality back and consume the encounter anyway
            RealityState.setMirrorLevel(0);
            ClientPlayNetworking.send(ManDialogueFinishedPayload.INSTANCE);
            finished = true;
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No dimmed background, the room stays visible
        int boxWidth = Math.min(this.width - 24, 380);
        int boxHeight = 107; // Keeps the asset's 3.55:1 proportions
        int x = (this.width - boxWidth) / 2;
        int y = this.height - boxHeight - 5;
        int c = CORNER_SRC; // 1:1, source pixels map straight to gui units

        // Interior first, frame over it. The white line sits 8px in, so the fill starts at 10
        graphics.fill(x + 10, y + 10, x + boxWidth - 10, y + boxHeight - 10, 0xFF000000);
        // Edges: border strips sampled between the corner sparkles, stretched
        graphics.blit(FRAME, x + c, y, boxWidth - 2 * c, BORDER, EDGE_AT, 0, EDGE_SRC, BORDER, TEX_W, TEX_H);
        graphics.blit(FRAME, x + c, y + boxHeight - BORDER, boxWidth - 2 * c, BORDER, EDGE_AT, TEX_H - BORDER, EDGE_SRC, BORDER, TEX_W, TEX_H);
        graphics.blit(FRAME, x, y + c, BORDER, boxHeight - 2 * c, 0, EDGE_AT, BORDER, EDGE_SRC, TEX_W, TEX_H);
        graphics.blit(FRAME, x + boxWidth - BORDER, y + c, BORDER, boxHeight - 2 * c, TEX_W - BORDER, EDGE_AT, BORDER, EDGE_SRC, TEX_W, TEX_H);
        // Corners, untouched source pixels
        graphics.blit(FRAME, x, y, c, c, 0, 0, CORNER_SRC, CORNER_SRC, TEX_W, TEX_H);
        graphics.blit(FRAME, x + boxWidth - c, y, c, c, TEX_W - CORNER_SRC, 0, CORNER_SRC, CORNER_SRC, TEX_W, TEX_H);
        graphics.blit(FRAME, x, y + boxHeight - c, c, c, 0, TEX_H - CORNER_SRC, CORNER_SRC, CORNER_SRC, TEX_W, TEX_H);
        graphics.blit(FRAME, x + boxWidth - c, y + boxHeight - c, c, c, TEX_W - CORNER_SRC, TEX_H - CORNER_SRC, CORNER_SRC, CORNER_SRC, TEX_W, TEX_H);

        String visible = lines.get(lineIndex).substring(0, (int) Math.min(revealed, lines.get(lineIndex).length()));
        int textX = x + 24;
        int textY = y + 22;
        int indent = this.font.width(styled("* "));
        int maxWidth = boxWidth - 48 - indent;

        List<String> rows = wrap(visible, maxWidth);
        for (int i = 0; i < rows.size(); i++) {
            String prefix = i == 0 ? "* " : "";
            int rowX = i == 0 ? textX : textX + indent;
            Component row = styled(prefix + rows.get(i));
            // Dark-navy shadow, then white text
            graphics.drawString(this.font, row, rowX + 1, textY + i * 18 + 1, SHADOW_COLOR, false);
            graphics.drawString(this.font, row, rowX, textY + i * 18, 0xFFFFFF, false);
        }
    }

    private static Component styled(String text) {
        return Component.literal(text).setStyle(FONT_STYLE);
    }

    // Wraps the same way at every reveal length, so shown text never reflows
    private List<String> wrap(String text, int maxWidth) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            String candidate = row.isEmpty() ? word : row + " " + word;
            if (this.font.width(styled(candidate)) > maxWidth && !row.isEmpty()) {
                rows.add(row.toString());
                row = new StringBuilder(word);
            } else {
                row = new StringBuilder(candidate);
            }
        }
        rows.add(row.toString());
        return rows;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // The mirrors keep moving while he speaks
    }
}
