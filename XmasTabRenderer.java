package dev.xmastab;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * Świąteczna lista graczy: złota ramka, cukrowe laski, migające lampki,
 * padający śnieg, choinki i odliczanie do Wigilii.
 * Wszystko rysowane zwykłymi fill(), więc bez tekstur i bardzo lekkie.
 */
public final class XmasTabRenderer {
    private XmasTabRenderer() {}

    // kolory (ARGB - w 26.x alfa musi być jawna)
    private static final int GOLD = 0xFFE9B93A;
    private static final int BG = 0xE6102B1B;
    private static final int RED = 0xFFC62828;
    private static final int WHITE = 0xFFF5F5F5;
    private static final int WIRE = 0xFF0B3D14;
    private static final int[] BULBS = {0xFFFF3B3B, 0xFF3BFF6A, 0xFFFFD23B, 0xFF3BA8FF};

    private static final int ROW_H = 12;
    private static final int PAD = 8;
    private static final int GAP = 6;
    private static final int HEADER_H = 40;
    private static final int FOOTER_H = 20;
    private static final int MAX_PLAYERS = 80;

    private record Entry(PlayerInfo info, Component name, String sortKey) {}

    public static void render(GuiGraphicsExtractor g, int screenWidth, Function<PlayerInfo, Component> nameFn) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return;
        Font font = mc.font;
        long t = System.currentTimeMillis();

        // --- gracze ---
        List<Entry> entries = new ArrayList<>();
        for (PlayerInfo info : conn.getListedOnlinePlayers()) {
            Component name = nameFn.apply(info);
            entries.add(new Entry(info, name, name.getString().toLowerCase(Locale.ROOT)));
        }
        entries.sort(Comparator.comparing(Entry::sortKey));
        int total = entries.size();

        // --- układ kolumn ---
        int rowsPerCol = Math.max(5, Math.min(20, (g.guiHeight() - 90) / ROW_H));
        int shown = Math.min(Math.min(total, 4 * rowsPerCol), MAX_PLAYERS);
        int cols = Math.max(1, (shown + rowsPerCol - 1) / rowsPerCol);
        rowsPerCol = Math.max(1, (shown + cols - 1) / cols);

        int colW = 90;
        for (int i = 0; i < shown; i++) {
            colW = Math.max(colW, font.width(entries.get(i).name()) + 14 + 18 + 6);
        }
        colW = Math.min(colW, 150);

        int contentW = cols * colW + (cols - 1) * GAP;
        int panelW = Math.max(contentW + 2 * PAD, 200);
        int x1 = (screenWidth - panelW) / 2;
        int x2 = x1 + panelW;
        int y1 = 6;
        int rowsY0 = y1 + HEADER_H;
        int rowsX0 = x1 + (panelW - contentW) / 2;
        int y2 = rowsY0 + rowsPerCol * ROW_H + FOOTER_H;

        // --- tło ---
        panel(g, x1, y1, x2, y2);
        snow(g, x1, y1, x2, y2, t);
        candy(g, x1 + 3, x2 - 3, y1 + 2);
        candy(g, x1 + 3, x2 - 3, y2 - 5);
        garland(g, x1 + 6, x2 - 6, y1 + 8, t);

        // --- nagłówek ---
        String title = "Wesołych Świąt!";
        int tw = font.width(title);
        int tx = x1 + (panelW - tw) / 2;
        g.text(font, title, tx, y1 + 18, GOLD, true);
        tree(g, tx - 12, y1 + 17);
        tree(g, tx + tw + 12, y1 + 17);

        String sub = countdown();
        g.text(font, sub, x1 + (panelW - font.width(sub)) / 2, y1 + 29, 0xFFFFC9C9, true);

        // --- wiersze graczy ---
        for (int i = 0; i < shown; i++) {
            int col = i / rowsPerCol;
            int row = i % rowsPerCol;
            int rx = rowsX0 + col * (colW + GAP);
            int ry = rowsY0 + row * ROW_H;
            Entry e = entries.get(i);

            int tint = (row % 2 == 0) ? 0x442E7D32 : 0x44C62828;
            g.fill(rx, ry, rx + colW, ry + ROW_H - 1, tint);

            PlayerFaceExtractor.extractRenderState(g, e.info().getSkin(), rx + 2, ry + 1, 8);
            g.text(font, e.name(), rx + 14, ry + 2, 0xFFFFFFFF, true);
            ping(g, rx + colW - 14, ry + 10, e.info().getLatency());
        }

        // --- stopka ---
        String foot = "Gracze online: " + total + (total > shown ? " (widać " + shown + ")" : "");
        g.text(font, foot, x1 + (panelW - font.width(foot)) / 2, rowsY0 + rowsPerCol * ROW_H + 4, 0xFFE8F5E9, true);
    }

    // ------------------------------------------------------------------ elementy

    private static void panel(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        g.fill(x1 + 2, y2, x2 + 2, y2 + 2, 0x55000000); // cień
        g.fill(x1 + 1, y1, x2 - 1, y2, GOLD);
        g.fill(x1, y1 + 1, x2, y2 - 1, GOLD);
        g.fill(x1 + 2, y1 + 3, x2 - 2, y2 - 3, BG);
        g.fill(x1 + 3, y1 + 2, x2 - 3, y2 - 2, BG);
    }

    /** Cukrowa laska: naprzemienne czerwone i białe odcinki. */
    private static void candy(GuiGraphicsExtractor g, int x1, int x2, int y) {
        int i = 0;
        for (int x = x1; x < x2; x += 4, i++) {
            g.fill(x, y, Math.min(x + 4, x2), y + 3, (i % 2 == 0) ? RED : WHITE);
        }
    }

    /** Łańcuch lampek - przewód z lekkim łukiem i migającymi bombkami. */
    private static void garland(GuiGraphicsExtractor g, int x1, int x2, int y, long t) {
        int spacing = 12;
        for (int px = x1; px < x2; px++) {
            double phase = ((px - x1) % spacing) / (double) spacing;
            int dy = (int) Math.round(Math.sin(phase * Math.PI) * 2);
            g.fill(px, y + dy, px + 1, y + dy + 1, WIRE);
        }
        int i = 0;
        for (int bx = x1; bx <= x2; bx += spacing, i++) {
            int base = BULBS[i % BULBS.length];
            boolean lit = ((t / 450 + i) % 2) == 0;
            if (lit) {
                g.fill(bx - 2, y, bx + 3, y + 6, (0x38 << 24) | (base & 0xFFFFFF)); // poświata
                g.fill(bx - 1, y + 1, bx + 2, y + 5, base);
            } else {
                g.fill(bx - 1, y + 1, bx + 2, y + 5, scale(base, 0.4));
            }
        }
    }

    /** Padający śnieg - deterministyczny, bez żadnego stanu. */
    private static void snow(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, long t) {
        int w = Math.max(1, x2 - x1 - 6);
        int h = Math.max(1, y2 - y1 - 6);
        for (int i = 0; i < 28; i++) {
            int hx = hash(i * 31 + 7);
            int hy = hash(i * 57 + 13);
            double speed = 6 + (hash(i * 91 + 5) & 7);
            double fy = (Math.floorMod(hy, h) + (t / 1000.0) * speed) % h;
            double sway = Math.sin(t / 900.0 + i) * 3;
            int px = x1 + 3 + (int) ((Math.floorMod(hx, w) + sway + w) % w);
            int py = y1 + 3 + (int) fy;
            int size = (i % 4 == 0) ? 2 : 1;
            g.fill(px, py, px + size, py + size, 0x99FFFFFF);
        }
    }

    /** Mała choinka z gwiazdką, ozdobami i pniem (ok. 9x13 px). */
    private static void tree(GuiGraphicsExtractor g, int cx, int y) {
        g.fill(cx, y - 2, cx + 1, y + 1, 0xFFFFE066);              // gwiazdka
        for (int h = 1; h <= 4; h++) {
            g.fill(cx - h, y + 2 * h - 1, cx + h + 1, y + 2 * h + 1, 0xFF2E9B3E);
        }
        g.fill(cx - 1, y + 9, cx + 2, y + 11, 0xFF6D4C41);         // pień
        g.fill(cx - 1, y + 4, cx, y + 5, 0xFFFF3B3B);              // bombki
        g.fill(cx + 1, y + 6, cx + 2, y + 7, 0xFFFFD23B);
        g.fill(cx - 2, y + 7, cx - 1, y + 8, 0xFF3BA8FF);
    }

    /** Cztery kreski jakości połączenia. */
    private static void ping(GuiGraphicsExtractor g, int x, int bottom, int ms) {
        int level = ms < 0 ? 0 : ms < 150 ? 4 : ms < 300 ? 3 : ms < 600 ? 2 : 1;
        int color = level >= 4 ? 0xFF4CAF50 : level == 3 ? 0xFF9CCC65 : level == 2 ? 0xFFFFC107 : 0xFFE53935;
        for (int b = 0; b < 4; b++) {
            int h = 2 + 2 * b;
            int bx = x + b * 3;
            g.fill(bx, bottom - h, bx + 2, bottom, b < level ? color : 0x55FFFFFF);
        }
    }

    // ------------------------------------------------------------------ pomocnicze

    private static String countdown() {
        LocalDate today = LocalDate.now();
        LocalDate eve = LocalDate.of(today.getYear(), 12, 24);
        long days = ChronoUnit.DAYS.between(today, eve);
        if (days < -2) { // po świętach - liczymy do następnych
            days = ChronoUnit.DAYS.between(today, LocalDate.of(today.getYear() + 1, 12, 24));
        }
        if (days == 0) return "Dziś Wigilia!";
        if (days == 1) return "Jutro Wigilia!";
        if (days < 0) return "Radosnych świąt!";
        return "Do Wigilii: " + days + " dni";
    }

    private static int scale(int argb, double f) {
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int gr = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private static int hash(int v) {
        v ^= v >>> 16;
        v *= 0x7feb352d;
        v ^= v >>> 15;
        v *= 0x846ca68b;
        v ^= v >>> 16;
        return v & 0x7fffffff;
    }
}
