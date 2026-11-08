package com.shiver.structurecompasslegacy.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.shiver.structurecompasslegacy.network.ClientRequest;
import com.shiver.structurecompasslegacy.network.CompassNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class CompassScreen extends GuiScreen {

    private static final int ROW_HEIGHT = 26;
    private static final int LIST_TOP = 56;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 4;
    private static final int MIN_THUMB_HEIGHT = 16;

    private final List<EntryItem> entries = new ArrayList<>();
    private final List<EntryItem> filtered = new ArrayList<>();
    private final int hand;

    private GuiTextField filter;
    private GuiButton search;
    private GuiButton cancel;
    private EntryItem selected;
    private NBTTagCompound progress = new NBTTagCompound();
    private String progressStructureId = "";
    private long requestId;

    private int scrollOffset;
    private int left;
    private int panelWidth;
    private int listBottom;
    private boolean draggingScrollbar;
    private int scrollbarDragOffset;
    private int lastClickedIndex = -1;
    private long lastClickTime;
    private List<String> pendingTooltip;

    public CompassScreen(NBTTagCompound catalog) {
        hand = catalog.getInteger("hand");
        Minecraft minecraft = Minecraft.getMinecraft();
        int playerDimension = minecraft.player != null ? minecraft.player.dimension : 0;
        NBTTagList structures = catalog.getTagList("structures", 10);
        for (int index = 0; index < structures.tagCount(); index++) {
            entries.add(new EntryItem(structures.getCompoundTagAt(index), playerDimension));
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        panelWidth = Math.min(500, width - 24);
        left = (width - panelWidth) / 2;

        String query = filter == null ? "" : filter.getText();
        filter = new GuiTextField(0, fontRenderer, left, 30, panelWidth, 20);
        filter.setMaxStringLength(100);
        filter.setText(query);
        filter.setFocused(true);

        int buttonWidth = (panelWidth - 12) / 3;
        int clearWidth = panelWidth - 2 * (buttonWidth + 6);
        buttonList.clear();
        search = new GuiButton(0, left, height - 28, buttonWidth, 20, ClientText.translate("search"));
        cancel = new GuiButton(1, left + buttonWidth + 6, height - 28, buttonWidth, 20, ClientText.translate("cancel"));
        buttonList.add(search);
        buttonList.add(cancel);
        buttonList.add(new GuiButton(2, left + 2 * (buttonWidth + 6), height - 28, clearWidth, 20,
                ClientText.translate("clear")));

        listBottom = Math.max(LIST_TOP + ROW_HEIGHT, height - 99);
        applyFilter(false);
    }

    private void applyFilter(boolean resetScroll) {
        String query = filter.getText().toLowerCase(Locale.ROOT).trim();
        filtered.clear();
        for (EntryItem entry : entries) {
            if (query.isEmpty() || entry.searchTerms.contains(query)) {
                filtered.add(entry);
            }
        }
        if (selected != null && !filtered.contains(selected)) {
            selected = null;
        }
        if (resetScroll) {
            scrollOffset = 0;
        } else {
            clampScroll();
        }
    }

    private int listContentWidth() {
        return Math.max(40, panelWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
    }

    private int viewHeight() {
        return Math.max(1, listBottom - LIST_TOP);
    }

    private int contentHeight() {
        return filtered.size() * ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - viewHeight());
    }

    private void clampScroll() {
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll());
    }

    private void ensureSelectedVisible() {
        if (selected == null) {
            return;
        }
        int index = filtered.indexOf(selected);
        if (index < 0) {
            return;
        }
        int rowTop = index * ROW_HEIGHT;
        int rowBottom = rowTop + ROW_HEIGHT;
        int view = viewHeight();
        if (rowTop < scrollOffset) {
            scrollOffset = rowTop;
        } else if (rowBottom > scrollOffset + view) {
            scrollOffset = rowBottom - view;
        }
        clampScroll();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (draggingScrollbar && !Mouse.isButtonDown(0)) {
            draggingScrollbar = false;
        }
        pendingTooltip = null;

        drawDefaultBackground();
        drawCenteredString(fontRenderer, ClientText.translate("title"), width / 2, 12, 0xFFFFFF);
        String count = filtered.size() + " / " + entries.size();
        int countWidth = fontRenderer.getStringWidth(count);
        fontRenderer.drawString(count, left + panelWidth - countWidth, 12, 0xAAAAAA);

        filter.drawTextBox();
        if (filter.getText().isEmpty()) {
            int hintX = left + (filter.isFocused() ? 8 : 4);
            String hint = fontRenderer.trimStringToWidth(ClientText.translate("search_hint"), panelWidth - 12);
            fontRenderer.drawString(hint, hintX, 36, 0x707070);
        }

        String dimLine = selected == null
                ? "" : ClientText.translate("dimension_label", selected.dimensionDisplay);
        String detail = selectedDetail();
        int detailHeight = computeDetailHeight(dimLine, detail);
        int detailTop = height - 72 - detailHeight;
        listBottom = Math.max(LIST_TOP + ROW_HEIGHT, detailTop - 6);
        clampScroll();

        drawList(mouseX, mouseY);
        drawScrollbar(mouseX, mouseY);
        drawDetail(dimLine, detail, detailTop);
        drawProgress();

        search.enabled = selected != null && selected.available;
        cancel.enabled = isSearching();
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            drawHoveringText(pendingTooltip, mouseX, mouseY);
        }
    }

    private int computeDetailHeight(String dimLine, String detail) {
        if (selected == null) {
            return 27;
        }
        int total = fontRenderer.getWordWrappedHeight(dimLine, panelWidth);
        if (!detail.isEmpty()) {
            total += 2 + fontRenderer.getWordWrappedHeight(detail, panelWidth);
        }
        return Math.max(27, total);
    }

    private void drawList(int mouseX, int mouseY) {
        int rowWidth = listContentWidth();
        int rowRight = left + rowWidth;
        if (filtered.isEmpty()) {
            fontRenderer.drawString(ClientText.translate("no_matches"), left + 5, LIST_TOP + 6, 0xBBBBBB);
            return;
        }

        beginScissor(left, LIST_TOP, rowRight, listBottom);
        try {
            int firstRow = Math.max(0, scrollOffset / ROW_HEIGHT);
            for (int index = firstRow; index < filtered.size(); index++) {
                int top = LIST_TOP + index * ROW_HEIGHT - scrollOffset;
                if (top >= listBottom) {
                    break;
                }
                int bottom = top + ROW_HEIGHT - 1;
                EntryItem entry = filtered.get(index);
                boolean isSelected = entry == selected;
                boolean isHovered = !draggingScrollbar && mouseX >= left && mouseX < rowRight
                        && mouseY >= Math.max(LIST_TOP, top) && mouseY < Math.min(listBottom, bottom);

                int bgColor = isSelected ? 0xB0556677 : (isHovered ? 0xA0303844 : 0x90202020);
                drawRect(left, top, rowRight, bottom, bgColor);

                int maxDimWidth = Math.max(60, rowWidth * 42 / 100);
                String dimText = entry.dimensionDisplay;
                boolean dimTrimmed = fontRenderer.getStringWidth(dimText) > maxDimWidth;
                if (dimTrimmed) {
                    dimText = trimWithEllipsis(dimText, maxDimWidth);
                }
                int dimWidth = fontRenderer.getStringWidth(dimText);
                int dimX = rowRight - 6 - dimWidth;
                int dimColor = entry.available ? 0xAADDAA : (entry.inCurrentDimension ? 0xFFCC77 : 0x999999);
                fontRenderer.drawString(dimText, dimX, top + 3, dimColor);

                int maxTitleWidth = Math.max(20, dimX - (left + 5) - 8);
                String titleText = entry.displayName;
                boolean titleTrimmed = fontRenderer.getStringWidth(titleText) > maxTitleWidth;
                if (titleTrimmed) {
                    titleText = trimWithEllipsis(titleText, maxTitleWidth);
                }
                int titleColor = entry.available ? 0xFFFFFF : 0x888888;
                fontRenderer.drawString(titleText, left + 5, top + 3, titleColor);

                String modText = fontRenderer.trimStringToWidth(entry.modName, Math.max(20, rowWidth - 10));
                fontRenderer.drawString(modText, left + 5, top + 15, entry.available ? 0xBBBBBB : 0x777777);

                if (isHovered) {
                    if (dimTrimmed && mouseX >= dimX && mouseX < rowRight && mouseY < top + 14) {
                        pendingTooltip = Collections.singletonList(
                                ClientText.translate("dimension_label", entry.dimensionDisplay));
                    } else if (titleTrimmed && mouseY < top + 14) {
                        pendingTooltip = Collections.singletonList(entry.displayName);
                    }
                }
            }
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void drawScrollbar(int mouseX, int mouseY) {
        int trackLeft = left + panelWidth - SCROLLBAR_WIDTH;
        int trackRight = left + panelWidth;
        int trackTop = LIST_TOP;
        int trackBottom = listBottom - 1;
        if (trackBottom <= trackTop) {
            return;
        }
        int max = maxScroll();
        drawRect(trackLeft, trackTop, trackRight, trackBottom, max > 0 ? 0x75000000 : 0x40000000);
        if (max <= 0) {
            return;
        }

        int trackHeight = trackBottom - trackTop;
        int thumbHeight = MathHelper.clamp(
                Math.round((float) viewHeight() * trackHeight / contentHeight()), MIN_THUMB_HEIGHT, trackHeight);
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbTop = trackTop + (travel == 0 ? 0 : Math.round((float) scrollOffset * travel / max));
        int thumbBottom = thumbTop + thumbHeight;

        boolean hoverThumb = mouseX >= trackLeft - 2 && mouseX <= trackRight + 2
                && mouseY >= thumbTop && mouseY <= thumbBottom;
        int bodyColor = draggingScrollbar ? 0xFFB0B0B0 : (hoverThumb ? 0xFF969696 : 0xFF808080);
        int highlightColor = draggingScrollbar || hoverThumb ? 0xFFE0E0E0 : 0xFFC0C0C0;
        drawRect(trackLeft, thumbTop, trackRight, thumbBottom, bodyColor);
        drawRect(trackLeft, thumbTop, trackRight - 1, thumbBottom - 1, highlightColor);
        drawRect(trackLeft + 1, thumbTop + 1, trackRight, thumbBottom, 0xFF555555);
        drawRect(trackLeft + 1, thumbTop + 1, trackRight - 1, thumbBottom - 1, bodyColor);
    }

    private void drawDetail(String dimLine, String detail, int detailTop) {
        if (selected == null) {
            fontRenderer.drawString(ClientText.translate("select_hint"), left, detailTop, 0x888888);
            return;
        }

        int y = detailTop;
        fontRenderer.drawSplitString(dimLine, left, y, panelWidth, 0xCCCCCC);
        if (!detail.isEmpty()) {
            y += fontRenderer.getWordWrappedHeight(dimLine, panelWidth) + 2;
            fontRenderer.drawSplitString(detail, left, y, panelWidth, 0xFFCC77);
        }
    }

    private String selectedDetail() {
        if (selected == null) {
            return "";
        }
        if (!selected.available) {
            return selected.unavailableReason;
        }
        return selected.limitation;
    }

    private void drawProgress() {
        if (progress.isEmpty()) {
            return;
        }

        String status = progress.getString("status");
        String text = ClientText.translate("status." + status);
        long total = progress.getLong("total");
        if (total > 0 && status.equals("running")) {
            text += " " + Math.min(100L, progress.getLong("checked") * 100L / total) + "%";
        }
        text += String.format(Locale.ROOT, " · %.1f s", progress.getLong("elapsed") / 1000.0D);
        EntryItem activeEntry = findEntry(progressStructureId);
        if (activeEntry != null && !status.equals("found")) {
            text = activeEntry.displayName + " · " + text;
        }
        fontRenderer.drawString(trimWithEllipsis(text, panelWidth), left, height - 66, 0xFFFFFF);

        if (status.equals("found")) {
            BlockPos position = BlockPos.fromLong(progress.getLong("target"));
            long distance = 0L;
            if (mc.player != null) {
                double deltaX = position.getX() + 0.5D - mc.player.posX;
                double deltaZ = position.getZ() + 0.5D - mc.player.posZ;
                distance = Math.round(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
            }
            String location = ClientText.translate(
                    "target", position.getX(), position.getY(), position.getZ(), distance);
            fontRenderer.drawString(trimWithEllipsis(location, panelWidth), left, height - 52, 0xAADDAA);
        } else if (!progress.getString("reason").isEmpty()) {
            String reason = progress.getString("reason");
            String key = "structurecompasslegacy.reason." + reason;
            String reasonText = I18n.hasKey(reason)
                    ? I18n.format(reason) : I18n.hasKey(key) ? I18n.format(key) : reason;
            fontRenderer.drawString(
                    fontRenderer.trimStringToWidth(reasonText, panelWidth), left, height - 52, 0xFFCC77);
        }
    }

    private EntryItem findEntry(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (EntryItem entry : entries) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private String trimWithEllipsis(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = fontRenderer.getStringWidth("...");
        if (maxWidth <= ellipsisWidth) {
            return fontRenderer.trimStringToWidth(text, maxWidth);
        }
        return fontRenderer.trimStringToWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    private void beginScissor(int scissorLeft, int scissorTop, int scissorRight, int scissorBottom) {
        ScaledResolution resolution = new ScaledResolution(mc);
        double scaleW = (double) mc.displayWidth / resolution.getScaledWidth_double();
        double scaleH = (double) mc.displayHeight / resolution.getScaledHeight_double();
        int x = (int) Math.floor(scissorLeft * scaleW);
        int y = (int) Math.floor(mc.displayHeight - scissorBottom * scaleH);
        int w = Math.max(0, (int) Math.ceil((scissorRight - scissorLeft) * scaleW));
        int h = Math.max(0, (int) Math.ceil((scissorBottom - scissorTop) * scaleH));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, w, h);
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

    private void startSearch() {
        if (selected == null || !selected.available) {
            return;
        }
        requestId = System.nanoTime();
        progressStructureId = selected.id;
        progress = new NBTTagCompound();
        progress.setString("status", "queued");
        CompassNetwork.CHANNEL.sendToServer(new ClientRequest(
                ClientRequest.SEARCH, hand, requestId, selected.id));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            startSearch();
        } else if (button.id == 1) {
            CompassNetwork.CHANNEL.sendToServer(new ClientRequest(ClientRequest.CANCEL, hand, requestId, ""));
        } else if (button.id == 2) {
            CompassNetwork.CHANNEL.sendToServer(new ClientRequest(ClientRequest.CLEAR_TARGET, hand, requestId, ""));
            progress = new NBTTagCompound();
            progressStructureId = "";
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 1 && mouseX >= left && mouseX < left + panelWidth && mouseY >= 30 && mouseY < 50
                && !filter.getText().isEmpty()) {
            filter.setText("");
            filter.setFocused(true);
            applyFilter(true);
            return;
        }

        filter.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }

        int trackLeft = left + panelWidth - SCROLLBAR_WIDTH;
        int trackRight = left + panelWidth;
        int trackTop = LIST_TOP;
        int trackBottom = listBottom - 1;
        if (trackBottom > trackTop && mouseX >= trackLeft - 2 && mouseX <= trackRight + 2
                && mouseY >= trackTop && mouseY <= trackBottom) {
            int max = maxScroll();
            if (max > 0) {
                int trackHeight = trackBottom - trackTop;
                int thumbHeight = MathHelper.clamp(
                        Math.round((float) viewHeight() * trackHeight / contentHeight()),
                        MIN_THUMB_HEIGHT, trackHeight);
                int travel = Math.max(0, trackHeight - thumbHeight);
                int thumbTop = trackTop + (travel == 0 ? 0 : Math.round((float) scrollOffset * travel / max));
                if (mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight) {
                    scrollbarDragOffset = mouseY - thumbTop;
                } else {
                    scrollbarDragOffset = thumbHeight / 2;
                    scrollFromTrackDrag(mouseY, trackTop, trackHeight, thumbHeight, max);
                }
                draggingScrollbar = true;
            }
            return;
        }

        int rowRight = left + listContentWidth();
        if (mouseX >= left && mouseX < rowRight && mouseY >= LIST_TOP && mouseY < listBottom) {
            int index = (mouseY - LIST_TOP + scrollOffset) / ROW_HEIGHT;
            if (index >= 0 && index < filtered.size()) {
                selectEntry(filtered.get(index));
                long now = Minecraft.getSystemTime();
                if (index == lastClickedIndex && now - lastClickTime < 250L && selected.available) {
                    startSearch();
                    lastClickedIndex = -1;
                } else {
                    lastClickedIndex = index;
                    lastClickTime = now;
                }
            }
        }
    }

    private void selectEntry(EntryItem entry) {
        if (selected != entry && !isSearching() && !entry.id.equals(progressStructureId)) {
            progress = new NBTTagCompound();
            progressStructureId = "";
        }
        selected = entry;
    }

    private void scrollFromTrackDrag(int mouseY, int trackTop, int trackHeight, int thumbHeight, int max) {
        int travel = Math.max(0, trackHeight - thumbHeight);
        if (travel <= 0) {
            scrollOffset = 0;
            return;
        }
        int targetThumbTop = MathHelper.clamp(mouseY - scrollbarDragOffset, trackTop, trackTop + travel);
        scrollOffset = MathHelper.clamp(Math.round((float) (targetThumbTop - trackTop) * max / travel), 0, max);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingScrollbar && clickedMouseButton == 0) {
            int max = maxScroll();
            int trackTop = LIST_TOP;
            int trackBottom = listBottom - 1;
            if (max > 0 && trackBottom > trackTop) {
                int trackHeight = trackBottom - trackTop;
                int thumbHeight = MathHelper.clamp(
                        Math.round((float) viewHeight() * trackHeight / contentHeight()),
                        MIN_THUMB_HEIGHT, trackHeight);
                scrollFromTrackDrag(mouseY, trackTop, trackHeight, thumbHeight, max);
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int delta = Math.round((-wheel / 120.0F) * ROW_HEIGHT);
            if (delta == 0) {
                delta = -Integer.signum(wheel) * (ROW_HEIGHT / 2);
            }
            scrollOffset = MathHelper.clamp(scrollOffset + delta, 0, maxScroll());
        }
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        if (filter.textboxKeyTyped(character, key)) {
            applyFilter(true);
            return;
        }
        if ((key == Keyboard.KEY_DOWN || key == Keyboard.KEY_UP) && !filtered.isEmpty()) {
            int current = selected == null ? -1 : filtered.indexOf(selected);
            int next;
            if (key == Keyboard.KEY_DOWN) {
                next = current < 0 ? 0 : Math.min(filtered.size() - 1, current + 1);
            } else {
                next = current < 0 ? 0 : Math.max(0, current - 1);
            }
            selectEntry(filtered.get(next));
            ensureSelectedVisible();
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (selected != null && selected.available) {
                startSearch();
                return;
            }
        }
        super.keyTyped(character, key);
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

    private static final class EntryItem {

        private final String id;
        private final String displayName;
        private final String modName;
        private final String dimensionDisplay;
        private final String searchTerms;
        private final boolean available;
        private final boolean inCurrentDimension;
        private final String limitation;
        private final String unavailableReason;

        private EntryItem(NBTTagCompound tag, int playerDimension) {
            id = tag.getString("id");
            String english = tag.getString("english");
            String chinese = tag.getString("chinese");
            displayName = ClientText.name(english, chinese);
            modName = tag.getString("mod");
            dimensionDisplay = ClientText.formatDimensions(tag);
            searchTerms = (english + " " + chinese + " " + modName + " " + id + " "
                    + ClientText.dimensionSearchTerms(tag)).toLowerCase(Locale.ROOT);
            available = tag.getBoolean("available");
            inCurrentDimension = ClientText.matchesDimension(tag, playerDimension);
            limitation = translateOptional(tag.getString("limitation"));
            unavailableReason = translateOptional(tag.getString("unavailableReason"));
        }

        private static String translateOptional(String key) {
            if (key == null || key.isEmpty()) {
                return "";
            }
            return I18n.hasKey(key) ? I18n.format(key) : key;
        }
    }
}
