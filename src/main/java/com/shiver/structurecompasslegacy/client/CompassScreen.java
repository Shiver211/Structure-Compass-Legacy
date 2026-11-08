package com.shiver.structurecompasslegacy.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.shiver.structurecompasslegacy.network.ClientRequest;
import com.shiver.structurecompasslegacy.network.CompassNetwork;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class CompassScreen extends GuiScreen {

    private static final int ROW_HEIGHT = 26;

    private final List<NBTTagCompound> entries = new ArrayList<>();
    private final List<NBTTagCompound> filtered = new ArrayList<>();
    private final int hand;

    private GuiTextField filter;
    private GuiButton search;
    private GuiButton cancel;
    private NBTTagCompound selected;
    private NBTTagCompound progress = new NBTTagCompound();
    private long requestId;
    private int offset;
    private int left;
    private int panelWidth;
    private int visibleRows;

    public CompassScreen(NBTTagCompound catalog) {
        hand = catalog.getInteger("hand");
        NBTTagList structures = catalog.getTagList("structures", 10);
        for (int index = 0; index < structures.tagCount(); index++) {
            entries.add(structures.getCompoundTagAt(index));
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        panelWidth = Math.min(500, width - 24);
        left = (width - panelWidth) / 2;
        visibleRows = Math.max(1, (height - 160) / ROW_HEIGHT);
        filter = new GuiTextField(0, fontRenderer, left, 30, panelWidth, 20);
        filter.setMaxStringLength(100);
        filter.setFocused(true);
        int buttonWidth = (panelWidth - 12) / 3;
        buttonList.clear();
        search = new GuiButton(0, left, height - 28, buttonWidth, 20, ClientText.translate("search"));
        cancel = new GuiButton(1, left + buttonWidth + 6, height - 28, buttonWidth, 20, ClientText.translate("cancel"));
        buttonList.add(search);
        buttonList.add(cancel);
        buttonList.add(new GuiButton(2, left + 2 * (buttonWidth + 6), height - 28, buttonWidth, 20,
                ClientText.translate("clear")));
        applyFilter();
    }

    private void applyFilter() {
        String query = filter.getText().toLowerCase(Locale.ROOT).trim();
        filtered.clear();
        for (NBTTagCompound entry : entries) {
            String terms = entry.getString("english") + " " + entry.getString("chinese") + " "
                    + entry.getString("mod") + " " + entry.getString("id");
            if (terms.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(entry);
            }
        }
        offset = 0;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, ClientText.translate("title"), width / 2, 12, 0xFFFFFF);
        filter.drawTextBox();

        String detail = selectedDetail();
        int detailHeight = Math.max(27, fontRenderer.getWordWrappedHeight(detail, panelWidth));
        int detailTop = height - 72 - detailHeight;
        visibleRows = Math.max(1, (detailTop - 62) / ROW_HEIGHT);
        offset = MathHelper.clamp(offset, 0, Math.max(0, filtered.size() - visibleRows));
        drawList();
        fontRenderer.drawSplitString(detail, left, detailTop, panelWidth, 0xFFCC77);
        drawProgress();
        search.enabled = selected != null && selected.getBoolean("available");
        cancel.enabled = isSearching();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawList() {
        for (int row = 0; row < visibleRows && row + offset < filtered.size(); row++) {
            NBTTagCompound entry = filtered.get(row + offset);
            int top = 56 + row * ROW_HEIGHT;
            drawRect(left, top, left + panelWidth, top + ROW_HEIGHT - 1,
                    entry == selected ? 0xB0556677 : 0x90202020);
            int color = entry.getBoolean("available") ? 0xFFFFFF : 0x888888;
            String name = ClientText.name(entry.getString("english"), entry.getString("chinese"));
            fontRenderer.drawString(name, left + 5, top + 3, color);
            fontRenderer.drawString(entry.getString("mod") + " · " + I18n.format(entry.getString("method")),
                    left + 5, top + 15, 0xBBBBBB);
        }
    }

    private String selectedDetail() {
        if (selected == null) {
            return "";
        }

        String detail = selected.getString("limitation");
        if (I18n.hasKey(detail)) {
            detail = I18n.format(detail);
        }
        if (!selected.getBoolean("available")) {
            String reason = selected.getString("unavailableReason");
            detail = I18n.hasKey(reason) ? I18n.format(reason) : reason;
        } else if (selected.getBoolean("experimental")) {
            detail = ClientText.translate("experimental") + " " + detail;
        }

        return detail;
    }

    private void drawProgress() {
        if (progress.isEmpty()) {
            return;
        }

        String status = progress.getString("status");
        String text = ClientText.translate("status." + status);
        long total = progress.getLong("total");
        if (total > 0 && status.equals("running")) {
            text += " " + progress.getLong("checked") * 100 / total + "%";
        }
        text += String.format(Locale.ROOT, " · %.1f s", progress.getLong("elapsed") / 1000.0D);
        fontRenderer.drawString(text, left, height - 66, 0xFFFFFF);

        if (status.equals("found")) {
            BlockPos position = BlockPos.fromLong(progress.getLong("target"));
            double deltaX = position.getX() + 0.5D - mc.player.posX;
            double deltaZ = position.getZ() + 0.5D - mc.player.posZ;
            String location = ClientText.translate("target", position.getX(), position.getY(), position.getZ(),
                    Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));
            fontRenderer.drawString(location, left, height - 52, 0xAADDAA);
        } else if (!progress.getString("reason").isEmpty()) {
            String reason = progress.getString("reason");
            String key = "structurecompasslegacy.reason." + reason;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(I18n.hasKey(key) ? I18n.format(key) : reason,
                    panelWidth), left, height - 52, 0xFFCC77);
        }
    }

    public void updateProgress(NBTTagCompound message) {
        if (message.getLong("request") == requestId) {
            progress = message;
        }
    }

    private boolean isSearching() {
        String status = progress.getString("status");
        return status.equals("running") || status.equals("queued");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && selected != null) {
            requestId = System.nanoTime();
            progress = new NBTTagCompound();
            progress.setString("status", "queued");
            CompassNetwork.CHANNEL.sendToServer(new ClientRequest(
                    ClientRequest.SEARCH, hand, requestId, selected.getString("id")));
        } else if (button.id == 1) {
            CompassNetwork.CHANNEL.sendToServer(new ClientRequest(ClientRequest.CANCEL, hand, requestId, ""));
        } else if (button.id == 2) {
            CompassNetwork.CHANNEL.sendToServer(new ClientRequest(ClientRequest.CLEAR_TARGET, hand, requestId, ""));
            progress = new NBTTagCompound();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        filter.mouseClicked(mouseX, mouseY, button);
        int row = (mouseY - 56) / ROW_HEIGHT;
        if (button == 0 && mouseX >= left && mouseX < left + panelWidth && mouseY >= 56
                && row < visibleRows && row + offset < filtered.size()) {
            selected = filtered.get(row + offset);
        }
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        if (filter.textboxKeyTyped(character, key)) {
            applyFilter();
        } else {
            super.keyTyped(character, key);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            offset = MathHelper.clamp(offset - Integer.signum(wheel), 0, Math.max(0, filtered.size() - visibleRows));
        }
    }

    @Override
    public void updateScreen() {
        filter.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
