package com.sw.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREF = "sw_launcher_v07_android16_expressive";
    private static final String SCREEN_HOME = "home";
    private static final String SCREEN_DRAWER = "drawer";
    private static final String SCREEN_SETTINGS = "settings";

    private FrameLayout root;
    private PackageManager pm;
    private SharedPreferences store;
    private final List<AppEntry> allApps = new ArrayList<>();

    private String currentScreen = SCREEN_HOME;
    private String drawerQuery = "";
    private String drawerCategory = "Todos";
    private int style = 1;
    private int columns = 4;
    private int iconSize = 56;
    private boolean showLabels = true;

    private static class AppEntry {
        String label;
        String pkg;
        String cls;
        Drawable icon;
        Intent launchIntent;
        String key() { return pkg + "/" + cls; }
    }

    private static class Palette {
        int bgA, bgB, surface, surfaceHigh, primary, onPrimary, text, sub, outline;
        boolean light;
        Palette(int bgA, int bgB, int surface, int surfaceHigh, int primary, int onPrimary, int text, int sub, int outline, boolean light) {
            this.bgA = bgA; this.bgB = bgB; this.surface = surface; this.surfaceHigh = surfaceHigh;
            this.primary = primary; this.onPrimary = onPrimary; this.text = text; this.sub = sub; this.outline = outline; this.light = light;
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        pm = getPackageManager();
        store = getSharedPreferences(PREF, MODE_PRIVATE);
        style = store.getInt("style", 0);
        columns = store.getInt("columns", 4);
        iconSize = store.getInt("iconSize", 56);
        showLabels = store.getBoolean("showLabels", true);
        loadApps();
        ensureFirstLayout();
        root = new FrameLayout(this);
        setContentView(root);
        showHome();
    }

    private Palette palette() {
        switch (style) {
            case 1: // Android Soft
                return new Palette(hex("#FFF8FB"), hex("#F5ECF4"), hex("#FFFBFE"), hex("#F0DEE9"), hex("#8A4D72"), Color.WHITE, hex("#211A20"), hex("#705B67"), hex("#E2CDD8"), true);
            case 2: // Android Bright
                return new Palette(hex("#F7FBFF"), hex("#E8F3FF"), hex("#FFFFFF"), hex("#DDEEFF"), hex("#006DCC"), Color.WHITE, hex("#0D1B2A"), hex("#536779"), hex("#C7DDF2"), true);
            case 3: // Android Bold
                return new Palette(hex("#15121C"), hex("#08070A"), hex("#211C2B"), hex("#342B46"), hex("#D0BCFF"), hex("#211C2B"), hex("#FFF7FF"), hex("#CAC1D7"), hex("#5E536B"), false);
            case 4: // AMOLED Ink
                return new Palette(Color.BLACK, hex("#020202"), hex("#0D0D0D"), hex("#1A1A1A"), hex("#BFD6FF"), hex("#07111E"), hex("#F7F7F7"), hex("#B4B4B4"), hex("#303030"), false);
            case 0: // Pixel Porcelain
            default:
                return new Palette(hex("#FAF8F3"), hex("#EEF3EE"), hex("#FFFDF8"), hex("#E4E9E3"), hex("#406651"), Color.WHITE, hex("#181D19"), hex("#5D665E"), hex("#D3DAD1"), true);
        }
    }

    private int hex(String value) { return Color.parseColor(value); }
    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    private int alpha(int rgb, int a) { return Color.argb(a, Color.red(rgb), Color.green(rgb), Color.blue(rgb)); }

    private void loadApps() {
        allApps.clear();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = pm.queryIntentActivities(i, 0);
        for (ResolveInfo r : result) {
            if (r.activityInfo == null) continue;
            AppEntry e = new AppEntry();
            e.label = r.loadLabel(pm).toString();
            e.pkg = r.activityInfo.packageName;
            e.cls = r.activityInfo.name;
            e.icon = r.loadIcon(pm);
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setComponent(new ComponentName(e.pkg, e.cls));
            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            e.launchIntent = launch;
            allApps.add(e);
        }
        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void ensureFirstLayout() {
        if (store.getBoolean("layoutReady", false)) return;

        List<String> dock = new ArrayList<>();
        addFirstFound(dock, 4, "com.whatsapp", "com.android.chrome", "com.google.android.gm", "com.instagram.android", "com.discord", "com.google.android.youtube", "com.android.vending");
        for (AppEntry e : allApps) {
            if (dock.size() >= 4) break;
            if (!dock.contains(e.key())) dock.add(e.key());
        }

        List<String> workspace = new ArrayList<>();
        addFirstFound(workspace, 12, "com.google.android.gm", "com.google.android.apps.photos", "com.android.vending", "com.android.chrome", "com.discord", "com.roblox.client", "com.supercell.brawlstars", "com.google.android.youtube");
        for (AppEntry e : allApps) {
            if (workspace.size() >= 8) break;
            if (!workspace.contains(e.key()) && !dock.contains(e.key())) workspace.add(e.key());
        }

        store.edit()
                .putString("workspace", join(workspace))
                .putString("dock", join(dock))
                .putBoolean("layoutReady", true)
                .apply();
    }

    private void addFirstFound(List<String> list, int max, String... packages) {
        for (String pkg : packages) {
            AppEntry e = findByPackage(pkg);
            if (e != null && !list.contains(e.key())) list.add(e.key());
            if (list.size() >= max) return;
        }
    }

    private String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append("||");
            sb.append(s);
        }
        return sb.toString();
    }

    private List<String> split(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        for (String s : raw.split("\\|\\|")) if (!s.trim().isEmpty()) out.add(s);
        return out;
    }

    private void putList(String key, List<String> list) { store.edit().putString(key, join(list)).apply(); }

    private AppEntry findByKey(String key) {
        for (AppEntry e : allApps) if (e.key().equals(key)) return e;
        return null;
    }

    private AppEntry findByPackage(String pkg) {
        for (AppEntry e : allApps) if (e.pkg.equals(pkg)) return e;
        return null;
    }

    private String label(AppEntry e) { return store.getString("name_" + e.key(), e.label); }
    private Set<String> hiddenSet() { return new HashSet<>(split(store.getString("hidden", ""))); }

    private GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable roundStroke(int color, float radius, int stroke) {
        GradientDrawable g = round(color, radius);
        g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView tv(String text, float size, int color, int typefaceStyle) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(true);
        if (typefaceStyle != 0) v.setTypeface(Typeface.DEFAULT, typefaceStyle);
        return v;
    }

    private TextView chip(String text, boolean selected) {
        Palette p = palette();
        TextView v = tv(text, 14, selected ? p.onPrimary : p.text, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(16), 0, dp(16), 0);
        v.setBackground(roundStroke(selected ? p.primary : alpha(p.surface, 218), 26, selected ? p.primary : p.outline));
        return v;
    }

    private TextView iconButton(String text) {
        Palette p = palette();
        TextView v = tv(text, 22, p.text, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setBackground(roundStroke(alpha(p.surface, 230), 26, p.outline));
        return v;
    }

    private void background() {
        Palette p = palette();
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{p.bgA, p.bgB});
        root.setBackground(bg);
    }

    private LinearLayout screenBase() {
        root.removeAllViews();
        background();
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(18), dp(14), dp(18), dp(10));
        main.setAlpha(0f);
        main.setTranslationY(dp(10));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));
        main.animate().alpha(1f).translationY(0).setDuration(190).start();
        return main;
    }

    private TextView pill(String text, int bg, int color) {
        Palette p = palette();
        TextView v = tv(text, 15, color, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(18), 0, dp(18), 0);
        v.setBackground(roundStroke(bg, 999, p.outline));
        return v;
    }

    private void microCard(LinearLayout parent, String title, String sub, String mark, Runnable action) {
        Palette p = palette();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundStroke(alpha(p.surface, p.light ? 232 : 218), 30, p.outline));
        TextView m = tv(mark, 22, p.primary, Typeface.BOLD);
        card.addView(m);
        card.addView(tv(title, 15, p.text, Typeface.BOLD));
        TextView small = tv(sub, 12, p.sub, 0);
        small.setMaxLines(1);
        card.addView(small);
        card.setOnClickListener(v -> action.run());
        card.setOnLongClickListener(v -> { action.run(); return true; });
        parent.addView(card, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void sectionTitle(LinearLayout parent, String text) {
        Palette p = palette();
        TextView t = tv(text, 13, p.sub, Typeface.BOLD);
        t.setText(text.toUpperCase(Locale.ROOT));
        t.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(4), dp(16), 0, dp(6));
        parent.addView(t, lp);
    }

    private void showHome() {
        currentScreen = SCREEN_HOME;
        Palette p = palette();
        LinearLayout main = screenBase();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(tv("SW", 13, p.primary, Typeface.BOLD));
        brand.addView(tv("Launcher", 24, p.text, Typeface.BOLD));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView drawerQuick = pill("Apps", alpha(p.surface, p.light ? 238 : 220), p.text);
        drawerQuick.setOnClickListener(v -> showDrawer());
        top.addView(drawerQuick, new LinearLayout.LayoutParams(dp(86), dp(48)));
        Space gap = new Space(this);
        top.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        TextView gear = iconButton("⚙");
        gear.setTextSize(19);
        gear.setOnClickListener(v -> showSettings());
        top.addView(gear, new LinearLayout.LayoutParams(dp(50), dp(48)));
        main.addView(top);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(20), dp(22), dp(18));
        hero.setBackground(roundStroke(alpha(p.surface, p.light ? 245 : 222), 38, p.outline));
        TextView time = tv(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), 54, p.text, Typeface.BOLD);
        time.setIncludeFontPadding(false);
        hero.addView(time);
        hero.addView(tv(new SimpleDateFormat("EEEE, dd MMM", new Locale("pt", "BR")).format(new Date()), 17, p.sub, Typeface.BOLD));
        TextView hint = tv("Seu espaço está pronto. Pesquise, organize e personalize.", 13, p.sub, 0);
        LinearLayout.LayoutParams hp0 = new LinearLayout.LayoutParams(-1, -2);
        hp0.setMargins(0, dp(8), 0, 0);
        hero.addView(hint, hp0);
        hero.setOnLongClickListener(v -> { showHomeEditSheet(); return true; });
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, dp(16), 0, dp(12));
        main.addView(hero, hp);

        TextView searchPill = tv("⌕  Pesquisar apps, atalhos e ajustes", 16, p.text, Typeface.BOLD);
        searchPill.setGravity(Gravity.CENTER_VERTICAL);
        searchPill.setPadding(dp(22), 0, dp(22), 0);
        searchPill.setBackground(roundStroke(alpha(p.surfaceHigh, p.light ? 238 : 220), 999, p.outline));
        searchPill.setOnClickListener(v -> showDrawer());
        main.addView(searchPill, new LinearLayout.LayoutParams(-1, dp(58)));

        LinearLayout cardsRow = new LinearLayout(this);
        cardsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams crp = new LinearLayout.LayoutParams(-1, -2);
        crp.setMargins(0, dp(12), 0, dp(8));
        main.addView(cardsRow, crp);
        microCard(cardsRow, "Gaveta", allApps.size() + " apps", "A", () -> showDrawer());
        Space sp1 = new Space(this); cardsRow.addView(sp1, new LinearLayout.LayoutParams(dp(10), 1));
        microCard(cardsRow, "Aparência", "cores e layout", "◇", () -> showStyleSheet());

        LinearLayout cardsRow2 = new LinearLayout(this);
        cardsRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams crp2 = new LinearLayout.LayoutParams(-1, -2);
        crp2.setMargins(0, 0, 0, dp(10));
        main.addView(cardsRow2, crp2);
        microCard(cardsRow2, "Editar", "área de trabalho", "+", () -> showHomeEditSheet());
        Space sp2 = new Space(this); cardsRow2.addView(sp2, new LinearLayout.LayoutParams(dp(10), 1));
        microCard(cardsRow2, "Ajustes", "launcher", "≡", () -> showSettings());

        LinearLayout rowTitle = new LinearLayout(this);
        rowTitle.setGravity(Gravity.CENTER_VERTICAL);
        rowTitle.addView(tv("Área de trabalho", 18, p.text, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView edit = tv("Editar", 13, p.primary, Typeface.BOLD);
        edit.setGravity(Gravity.CENTER);
        edit.setOnClickListener(v -> showHomeEditSheet());
        rowTitle.addView(edit, new LinearLayout.LayoutParams(dp(78), dp(38)));
        main.addView(rowTitle);

        ScrollView area = new ScrollView(this);
        area.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.VERTICAL);
        area.addView(workspace);
        main.addView(area, new LinearLayout.LayoutParams(-1, 0, 1));

        List<String> keys = split(store.getString("workspace", ""));
        if (keys.isEmpty()) {
            emptyWorkspace(workspace);
        } else {
            appGrid(workspace, keys, columns, false);
        }

        addDock(main);
    }

    private void emptyWorkspace(LinearLayout parent) {
        Palette p = palette();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(40), dp(24), dp(40));
        card.setBackground(roundStroke(alpha(p.surface, 230), 32, p.outline));
        card.addView(tv("Sua área está limpa", 23, p.text, Typeface.BOLD));
        TextView sub = tv("Adicione apps da gaveta para montar seu espaço.", 14, p.sub, 0);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub);
        TextView add = chip("Adicionar apps", true);
        add.setOnClickListener(v -> showDrawer());
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(56));
        ap.setMargins(0, dp(20), 0, 0);
        card.addView(add, ap);
        parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addDock(LinearLayout main) {
        Palette p = palette();
        LinearLayout dockWrap = new LinearLayout(this);
        dockWrap.setOrientation(LinearLayout.VERTICAL);
        dockWrap.setPadding(dp(8), dp(6), dp(8), dp(6));
        dockWrap.setBackground(roundStroke(alpha(p.surface, p.light ? 245 : 226), 999, p.outline));

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dockWrap.addView(dock, new LinearLayout.LayoutParams(-1, -1));

        List<String> dockKeys = split(store.getString("dock", ""));
        for (int i = 0; i < 2; i++) {
            if (i < dockKeys.size()) {
                AppEntry e = findByKey(dockKeys.get(i));
                dock.addView(e == null ? spacer() : appItem(e, true), new LinearLayout.LayoutParams(0, -1, 1));
            } else dock.addView(spacer(), new LinearLayout.LayoutParams(0, -1, 1));
        }

        TextView drawer = tv("⌕", 30, p.onPrimary, Typeface.BOLD);
        drawer.setGravity(Gravity.CENTER);
        drawer.setBackground(round(p.primary, 999));
        drawer.setOnClickListener(v -> showDrawer());
        drawer.setOnLongClickListener(v -> { showSettings(); return true; });
        LinearLayout.LayoutParams center = new LinearLayout.LayoutParams(0, dp(62), 1);
        center.setMargins(dp(6), 0, dp(6), 0);
        dock.addView(drawer, center);

        for (int i = 2; i < 4; i++) {
            if (i < dockKeys.size()) {
                AppEntry e = findByKey(dockKeys.get(i));
                dock.addView(e == null ? spacer() : appItem(e, true), new LinearLayout.LayoutParams(0, -1, 1));
            } else dock.addView(spacer(), new LinearLayout.LayoutParams(0, -1, 1));
        }

        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, dp(78));
        dpv.setMargins(0, dp(8), 0, 0);
        main.addView(dockWrap, dpv);
    }

    private View spacer() { return new Space(this); }

    private void appGrid(LinearLayout parent, List<String> keys, int cols, boolean drawer) {
        LinearLayout row = null;
        int index = 0;
        for (String key : keys) {
            AppEntry e = findByKey(key);
            if (e == null) continue;
            if (index % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                rp.setMargins(0, dp(8), 0, dp(8));
                parent.addView(row, rp);
            }
            row.addView(appItem(e, false), new LinearLayout.LayoutParams(0, -2, 1));
            index++;
        }
        if (row != null && index % cols != 0) {
            int missing = cols - (index % cols);
            for (int i = 0; i < missing; i++) row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1));
        }
    }

    private View appItem(AppEntry e, boolean dock) {
        Palette p = palette();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(3), dp(6), dp(3), dp(6));

        FrameLayout iconPlate = new FrameLayout(this);
        iconPlate.setBackground(round(alpha(p.surfaceHigh, p.light ? 170 : 120), dock ? 22 : 24));
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(e.icon);
        int size = dp(dock ? Math.max(42, iconSize - 12) : iconSize);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
        iconPlate.addView(icon, ilp);
        int plate = dp(dock ? Math.max(50, iconSize - 2) : iconSize + 14);
        box.addView(iconPlate, new LinearLayout.LayoutParams(plate, plate));

        if (showLabels && !dock) {
            TextView label = tv(label(e), 12, p.text, 0);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(5), 0, 0);
            box.addView(label, lp);
        }
        box.setOnClickListener(v -> {
            box.animate().scaleX(0.94f).scaleY(0.94f).setDuration(65).withEndAction(() -> {
                box.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                openApp(e);
            }).start();
        });
        box.setOnLongClickListener(v -> { showAppSheet(e); return true; });
        return box;
    }

    private void showDrawer() {
        currentScreen = SCREEN_DRAWER;
        drawerQuery = "";
        drawerCategory = "Todos";
        renderDrawer();
    }

    private void renderDrawer() {
        Palette p = palette();
        LinearLayout main = screenBase();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = iconButton("‹");
        back.setTextSize(30);
        back.setOnClickListener(v -> showHome());
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(52)));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.setPadding(dp(12), 0, 0, 0);
        title.addView(tv("Apps", 28, p.text, Typeface.BOLD));
        title.addView(tv(allApps.size() + " apps instalados", 13, p.sub, 0));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        main.addView(header);

        EditText search = new EditText(this);
        search.setHint("Pesquisar apps");
        search.setText(drawerQuery);
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setTextColor(p.text);
        search.setHintTextColor(p.sub);
        search.setPadding(dp(22), 0, dp(22), 0);
        search.setBackground(roundStroke(alpha(p.surface, 238), 30, p.outline));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(62));
        sp.setMargins(0, dp(18), 0, dp(12));
        main.addView(search, sp);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        String[] cats = {"Todos", "Recentes", "Usados", "Jogos", "Social", "Mídia", "Ferramentas", "Sistema", "Ocultos"};
        for (String cat : cats) {
            TextView c = chip(cat, cat.equals(drawerCategory));
            c.setOnClickListener(v -> { drawerCategory = ((TextView) v).getText().toString(); renderDrawerKeepingQuery(); });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(48));
            cp.setMargins(0, 0, dp(8), 0);
            chips.addView(c, cp);
        }
        hsv.addView(chips);
        main.addView(hsv);

        FrameLayout area = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        area.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        main.addView(area, new LinearLayout.LayoutParams(-1, 0, 1));
        fillDrawer(content);
        addAlphabetRail(area);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                drawerQuery = s.toString();
                content.removeAllViews();
                fillDrawer(content);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void renderDrawerKeepingQuery() {
        String q = drawerQuery;
        renderDrawer();
        drawerQuery = q;
    }

    private void fillDrawer(LinearLayout content) {
        List<String> keys = new ArrayList<>();
        Set<String> hidden = hiddenSet();
        String q = drawerQuery == null ? "" : drawerQuery.trim().toLowerCase(Locale.ROOT);

        if ("Recentes".equals(drawerCategory) || "Usados".equals(drawerCategory)) {
            keys.addAll(split(store.getString("recent", "")));
        } else {
            for (AppEntry e : allApps) {
                boolean isHidden = hidden.contains(e.key());
                if ("Ocultos".equals(drawerCategory)) {
                    if (!isHidden) continue;
                } else if (isHidden) {
                    continue;
                }
                if (!"Todos".equals(drawerCategory) && !"Ocultos".equals(drawerCategory)) {
                    if (!categoryOf(e).equals(drawerCategory)) continue;
                }
                if (!q.isEmpty() && !label(e).toLowerCase(Locale.ROOT).contains(q)) continue;
                keys.add(e.key());
            }
        }

        if (keys.isEmpty()) {
            emptyList(content, "Nada por aqui", "Use outra categoria ou pesquise outro nome.");
        } else {
            appGrid(content, keys, columns, true);
        }
    }

    private String categoryOf(AppEntry e) {
        String n = (e.pkg + " " + e.label).toLowerCase(Locale.ROOT);
        if (n.contains("roblox") || n.contains("game") || n.contains("supercell") || n.contains("brawl") || n.contains("gaming")) return "Jogos";
        if (n.contains("whatsapp") || n.contains("discord") || n.contains("instagram") || n.contains("facebook") || n.contains("tiktok") || n.contains("gmail") || n.contains("telegram")) return "Social";
        if (n.contains("youtube") || n.contains("spotify") || n.contains("music") || n.contains("camera") || n.contains("gallery") || n.contains("foto") || n.contains("capcut")) return "Mídia";
        if (n.contains("setting") || n.contains("config") || n.contains("calculator") || n.contains("files") || n.contains("clock") || n.contains("calendar") || n.contains("maps")) return "Ferramentas";
        if (e.pkg.startsWith("com.android") || e.pkg.startsWith("com.samsung") || e.pkg.startsWith("com.google.android")) return "Sistema";
        return "Ferramentas";
    }

    private void emptyList(LinearLayout parent, String title, String sub) {
        Palette p = palette();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(22), dp(54), dp(22), dp(54));
        card.setBackground(roundStroke(alpha(p.surface, 220), 30, p.outline));
        card.addView(tv(title, 22, p.text, Typeface.BOLD));
        TextView s = tv(sub, 14, p.sub, 0);
        s.setGravity(Gravity.CENTER);
        card.addView(s);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(20), dp(44), 0);
        parent.addView(card, cp);
    }

    private void addAlphabetRail(FrameLayout parent) {
        Palette p = palette();
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER);
        rail.setPadding(dp(3), dp(8), dp(3), dp(8));
        rail.setBackground(roundStroke(alpha(p.surface, p.light ? 210 : 170), 22, p.outline));
        String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < abc.length(); i++) {
            char ch = abc.charAt(i);
            TextView t = tv(String.valueOf(ch), 10, p.sub, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            final char selected = ch;
            t.setOnClickListener(v -> Toast.makeText(this, "Letra " + selected, Toast.LENGTH_SHORT).show());
            rail.addView(t, new LinearLayout.LayoutParams(dp(28), 0, 1));
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(34), -1, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        lp.setMargins(0, dp(24), 0, dp(20));
        parent.addView(rail, lp);
    }

    private void showSettings() {
        currentScreen = SCREEN_SETTINGS;
        Palette p = palette();
        LinearLayout main = screenBase();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = iconButton("‹");
        back.setTextSize(30);
        back.setOnClickListener(v -> showHome());
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(52)));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.setPadding(dp(12), 0, 0, 0);
        title.addView(tv("Configurações", 30, p.text, Typeface.BOLD));
        title.addView(tv("Personalização, layout, dock e integração", 13, p.sub, 0));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        main.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        main.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        settingRow(list, "Tela inicial", "Colunas, apps fixados e edição da área", () -> showHomeEditSheet());
        settingRow(list, "Gaveta de apps", "Busca, categorias, ocultos e rolagem A-Z", () -> showDrawer());
        settingRow(list, "Dock", "Segure apps para adicionar ou remover do dock", () -> Toast.makeText(this, "Segure um app para editar o dock.", Toast.LENGTH_LONG).show());
        settingRow(list, "Aparência", "Neutral, Soft, Bright, Bold e AMOLED", () -> showStyleSheet());
        settingRow(list, "Colunas da grade", "Atual: " + columns, () -> cycleColumns());
        settingRow(list, "Tamanho dos ícones", "Atual: " + iconSize + "dp", () -> cycleIconSize());
        settingRow(list, "Nomes dos apps", showLabels ? "Ativados" : "Ocultos", () -> toggleLabels());
        settingRow(list, "Permissão de uso real", "Ativar estatísticas reais de uso do Android", () -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        settingRow(list, "Recarregar apps", allApps.size() + " apps encontrados", () -> { loadApps(); Toast.makeText(this, "Apps recarregados", Toast.LENGTH_SHORT).show(); showSettings(); });
        settingRow(list, "Resetar layout", "Limpa área, dock, nomes e ocultos", () -> resetLayout());
        settingRow(list, "Sobre", "SW Launcher v0.7 Android 16 Expressive UI", () -> Toast.makeText(this, "SW Launcher v0.7 Android 16 Expressive UI", Toast.LENGTH_LONG).show());
    }

    private void settingRow(LinearLayout parent, String title, String sub, Runnable action) {
        Palette p = palette();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundStroke(alpha(p.surface, p.light ? 238 : 220), 30, p.outline));

        TextView badge = tv(title.substring(0, 1), 18, p.onPrimary, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(p.primary, 999));
        card.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(14), 0, dp(10), 0);
        texts.addView(tv(title, 17, p.text, Typeface.BOLD));
        TextView subText = tv(sub, 13, p.sub, 0);
        subText.setMaxLines(2);
        texts.addView(subText);
        card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arrow = tv("›", 28, p.sub, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(26), -1));
        card.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(12), 0, 0);
        parent.addView(card, lp);
    }

    private void showHomeEditSheet() {
        Dialog d = bottomSheet();
        LinearLayout box = sheetContent();
        Palette p = palette();
        box.addView(tv("Editar área", 26, p.text, Typeface.BOLD));
        box.addView(tv("Monte sua tela inicial sem misturar com a gaveta.", 14, p.sub, 0));
        divider(box);
        box.addView(sheetOption("Adicionar apps", () -> { d.dismiss(); showDrawer(); }));
        box.addView(sheetOption("Mudar estilo", () -> { d.dismiss(); showStyleSheet(); }));
        box.addView(sheetOption("Ajustar colunas", () -> { d.dismiss(); cycleColumns(); showHome(); }));
        box.addView(sheetOption("Tamanho dos ícones", () -> { d.dismiss(); cycleIconSize(); showHome(); }));
        box.addView(sheetOption("Limpar área de trabalho", () -> { d.dismiss(); putList("workspace", new ArrayList<>()); showHome(); }));
        d.setContentView(box);
        showBottom(d);
    }

    private void showStyleSheet() {
        Dialog d = bottomSheet();
        LinearLayout box = sheetContent();
        Palette p = palette();
        box.addView(tv("Aparência", 26, p.text, Typeface.BOLD));
        box.addView(tv("Cores tonais inspiradas no visual do Android 16.", 14, p.sub, 0));
        divider(box);
        String[] names = {"Pixel Porcelain", "Android Soft", "Android Bright", "Android Bold", "AMOLED Ink"};
        for (int i = 0; i < names.length; i++) {
            final int selected = i;
            box.addView(sheetOption((style == i ? "✓ " : "") + names[i], () -> { style = selected; store.edit().putInt("style", selected).apply(); d.dismiss(); refresh(); }));
        }
        d.setContentView(box);
        showBottom(d);
    }

    private void showAppSheet(AppEntry e) {
        Dialog d = bottomSheet();
        LinearLayout box = sheetContent();
        Palette p = palette();

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView img = new ImageView(this);
        img.setImageDrawable(e.icon);
        head.addView(img, new LinearLayout.LayoutParams(dp(62), dp(62)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(16), 0, 0, 0);
        texts.addView(tv(label(e), 22, p.text, Typeface.BOLD));
        texts.addView(tv(e.pkg, 12, p.sub, 0));
        head.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(head);
        divider(box);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(actionButton("Abrir", () -> { d.dismiss(); openApp(e); }), new LinearLayout.LayoutParams(0, dp(56), 1));
        actions.addView(actionButton(inList("workspace", e.key()) ? "Remover" : "Fixar", () -> { d.dismiss(); toggleList("workspace", e.key(), 48); refresh(); }), new LinearLayout.LayoutParams(0, dp(56), 1));
        actions.addView(actionButton("Info", () -> { d.dismiss(); openInfo(e); }), new LinearLayout.LayoutParams(0, dp(56), 1));
        box.addView(actions);

        box.addView(sheetOption("Editar nome exibido", () -> { d.dismiss(); rename(e); }));
        box.addView(sheetOption(inList("dock", e.key()) ? "Remover do dock" : "Adicionar ao dock", () -> { d.dismiss(); toggleList("dock", e.key(), 4); refresh(); }));
        box.addView(sheetOption("Mover na área", () -> { d.dismiss(); moveInWorkspace(e); refresh(); }));
        box.addView(sheetOption(hiddenSet().contains(e.key()) ? "Mostrar na gaveta" : "Ocultar da gaveta", () -> { d.dismiss(); toggleHidden(e); refresh(); }));
        box.addView(sheetOption("Cache e dados", () -> { d.dismiss(); openInfo(e); }));
        box.addView(sheetOption("Desinstalar", () -> { d.dismiss(); uninstall(e); }));

        d.setContentView(box);
        showBottom(d);
    }

    private Dialog bottomSheet() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    private LinearLayout sheetContent() {
        Palette p = palette();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(22), dp(24), dp(24));
        box.setBackground(roundStroke(p.surface, 34, p.outline));
        return box;
    }

    private void showBottom(Dialog d) {
        d.setOnShowListener(dialog -> {
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(w.getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.BOTTOM;
                w.setAttributes(lp);
            }
        });
        d.show();
    }

    private void divider(LinearLayout box) {
        Palette p = palette();
        View line = new View(this);
        line.setBackgroundColor(p.outline);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(16), 0, dp(14));
        box.addView(line, lp);
    }

    private TextView sheetOption(String text, Runnable run) {
        Palette p = palette();
        TextView row = tv(text, 17, p.text, Typeface.BOLD);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), 0, dp(20), 0);
        row.setBackground(roundStroke(alpha(p.surfaceHigh, p.light ? 245 : 230), 26, p.outline));
        row.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58));
        lp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(lp);
        return row;
    }

    private TextView actionButton(String text, Runnable run) {
        Palette p = palette();
        TextView b = tv(text, 14, p.text, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(roundStroke(alpha(p.surfaceHigh, p.light ? 245 : 230), 24, p.outline));
        b.setOnClickListener(v -> run.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(56), 1);
        lp.setMargins(dp(4), 0, dp(4), dp(12));
        b.setLayoutParams(lp);
        return b;
    }

    private boolean inList(String listKey, String value) { return split(store.getString(listKey, "")).contains(value); }

    private void toggleList(String listKey, String value, int max) {
        List<String> list = split(store.getString(listKey, ""));
        if (list.contains(value)) list.remove(value);
        else {
            if (list.size() >= max) list.remove(0);
            list.add(value);
        }
        putList(listKey, list);
    }

    private void toggleHidden(AppEntry e) {
        Set<String> set = hiddenSet();
        if (set.contains(e.key())) set.remove(e.key()); else set.add(e.key());
        putList("hidden", new ArrayList<>(set));
    }

    private void moveInWorkspace(AppEntry e) {
        List<String> list = split(store.getString("workspace", ""));
        int idx = list.indexOf(e.key());
        if (idx < 0) {
            Toast.makeText(this, "Fixe o app na área primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idx > 0) Collections.swap(list, idx, idx - 1);
        else if (list.size() > 1) Collections.swap(list, idx, idx + 1);
        putList("workspace", list);
    }

    private void rename(AppEntry e) {
        EditText input = new EditText(this);
        input.setText(label(e));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Nome exibido")
                .setView(input)
                .setPositiveButton("Salvar", (d, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = e.label;
                    store.edit().putString("name_" + e.key(), name).apply();
                    refresh();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cycleColumns() {
        columns++;
        if (columns > 5) columns = 3;
        store.edit().putInt("columns", columns).apply();
        Toast.makeText(this, "Colunas: " + columns, Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void cycleIconSize() {
        iconSize += 4;
        if (iconSize > 72) iconSize = 48;
        store.edit().putInt("iconSize", iconSize).apply();
        Toast.makeText(this, "Ícones: " + iconSize + "dp", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void toggleLabels() {
        showLabels = !showLabels;
        store.edit().putBoolean("showLabels", showLabels).apply();
        refresh();
    }

    private void resetLayout() {
        store.edit().clear().apply();
        style = 0;
        columns = 4;
        iconSize = 56;
        showLabels = true;
        ensureFirstLayout();
        Toast.makeText(this, "Layout resetado", Toast.LENGTH_SHORT).show();
        showHome();
    }

    private void openApp(AppEntry e) {
        try {
            startActivity(e.launchIntent);
            List<String> recent = split(store.getString("recent", ""));
            recent.remove(e.key());
            recent.add(0, e.key());
            while (recent.size() > 30) recent.remove(recent.size() - 1);
            putList("recent", recent);
        } catch (Exception ex) {
            Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show();
        }
    }

    private void openInfo(AppEntry e) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + e.pkg));
        startActivity(i);
    }

    private void uninstall(AppEntry e) {
        Intent i = new Intent(Intent.ACTION_DELETE);
        i.setData(Uri.parse("package:" + e.pkg));
        startActivity(i);
    }

    private void refresh() {
        if (SCREEN_DRAWER.equals(currentScreen)) renderDrawer();
        else if (SCREEN_SETTINGS.equals(currentScreen)) showSettings();
        else showHome();
    }

    @Override
    public void onBackPressed() {
        if (!SCREEN_HOME.equals(currentScreen)) showHome(); else super.onBackPressed();
    }
}
