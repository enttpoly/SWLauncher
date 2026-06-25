package com.sw.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
    private static final String PREF = "sw_launcher_v06";
    private static final String MODE_HOME = "home";
    private static final String MODE_DRAWER = "drawer";
    private static final String MODE_SETTINGS = "settings";

    private FrameLayout root;
    private PackageManager pm;
    private SharedPreferences sp;
    private final List<AppEntry> allApps = new ArrayList<>();
    private String currentMode = MODE_HOME;
    private String drawerQuery = "";
    private int style = 1;
    private int columns = 4;
    private int iconSize = 58;

    private static class AppEntry {
        String label;
        String pkg;
        String cls;
        Drawable icon;
        Intent launchIntent;

        String key() {
            return pkg + "/" + cls;
        }
    }

    private static class Palette {
        int bgTop;
        int bgBottom;
        int surface;
        int surface2;
        int primary;
        int onPrimary;
        int text;
        int sub;
        int stroke;

        Palette(int bgTop, int bgBottom, int surface, int surface2, int primary, int onPrimary, int text, int sub, int stroke) {
            this.bgTop = bgTop;
            this.bgBottom = bgBottom;
            this.surface = surface;
            this.surface2 = surface2;
            this.primary = primary;
            this.onPrimary = onPrimary;
            this.text = text;
            this.sub = sub;
            this.stroke = stroke;
        }
    }

    private Palette p() {
        switch (style) {
            case 0: // Neutral
                return new Palette(c("#F8F7F3"), c("#ECE8DF"), c("#F2EFE7"), c("#E5E0D4"), c("#5C5A55"), Color.WHITE, c("#171715"), c("#65625C"), c("#D5CFC2"));
            case 2: // Bright
                return new Palette(c("#F8FBFF"), c("#E7F2FF"), c("#F3F8FF"), c("#D9EAFE"), c("#1167D8"), Color.WHITE, c("#0B1B2F"), c("#52657C"), c("#C7DCF5"));
            case 3: // Bold
                return new Palette(c("#160C1F"), c("#07070B"), c("#24172F"), c("#352043"), c("#D7B4FF"), c("#23142F"), c("#F9F0FF"), c("#C9B9D4"), c("#6A5478"));
            case 4: // AMOLED
                return new Palette(Color.BLACK, c("#050505"), c("#111111"), c("#1D1D1D"), c("#E9E5FF"), c("#101010"), c("#F5F5F5"), c("#A5A5A5"), c("#333333"));
            case 1: // Soft
            default:
                return new Palette(c("#F9F4FF"), c("#EAE1F5"), c("#F7F0FF"), c("#E8DDF2"), c("#6D4E8E"), Color.WHITE, c("#1D1724"), c("#6B6075"), c("#D8CBE4"));
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        pm = getPackageManager();
        sp = getSharedPreferences(PREF, MODE_PRIVATE);
        style = sp.getInt("style", 1);
        columns = sp.getInt("columns", 4);
        iconSize = sp.getInt("iconSize", 58);
        loadApps();
        ensureDefaultLayout();
        root = new FrameLayout(this);
        setContentView(root);
        showHome();
    }

    private int c(String hex) {
        return Color.parseColor(hex);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void loadApps() {
        allApps.clear();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo r : resolved) {
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

    private void ensureDefaultLayout() {
        if (sp.getBoolean("layoutReady", false)) return;
        List<String> workspace = new ArrayList<>();
        String[] wantedHome = {"com.google.android.gm", "com.google.android.apps.photos", "com.google.android.googlequicksearchbox", "com.android.vending", "com.android.chrome"};
        for (String pkg : wantedHome) {
            AppEntry e = findByPackage(pkg);
            if (e != null && !workspace.contains(e.key())) workspace.add(e.key());
        }
        for (AppEntry e : allApps) {
            if (workspace.size() >= 8) break;
            if (!workspace.contains(e.key())) workspace.add(e.key());
        }

        List<String> dock = new ArrayList<>();
        String[] wantedDock = {"com.google.android.dialer", "com.samsung.android.dialer", "com.whatsapp", "com.instagram.android", "com.android.chrome", "com.discord"};
        for (String pkg : wantedDock) {
            AppEntry e = findByPackage(pkg);
            if (e != null && !dock.contains(e.key())) dock.add(e.key());
            if (dock.size() >= 4) break;
        }
        for (AppEntry e : allApps) {
            if (dock.size() >= 4) break;
            if (!dock.contains(e.key())) dock.add(e.key());
        }

        sp.edit()
                .putString("workspace", join(workspace))
                .putString("dock", join(dock))
                .putBoolean("layoutReady", true)
                .apply();
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
        String[] parts = raw.split("\\|\\|");
        for (String s : parts) if (!s.trim().isEmpty()) out.add(s);
        return out;
    }

    private void putList(String key, List<String> list) {
        sp.edit().putString(key, join(list)).apply();
    }

    private AppEntry findByKey(String key) {
        for (AppEntry e : allApps) if (e.key().equals(key)) return e;
        return null;
    }

    private AppEntry findByPackage(String pkg) {
        for (AppEntry e : allApps) if (e.pkg.equals(pkg)) return e;
        return null;
    }

    private String customLabel(AppEntry e) {
        return sp.getString("name_" + e.key(), e.label);
    }

    private Set<String> hiddenSet() {
        return new HashSet<>(split(sp.getString("hidden", "")));
    }

    private GradientDrawable shape(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable shapeStroke(int color, float radius, int strokeColor) {
        GradientDrawable g = shape(color, radius);
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private TextView text(String t, float size, int color, int styleType) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(true);
        if (styleType != 0) v.setTypeface(Typeface.DEFAULT, styleType);
        return v;
    }

    private TextView pill(String t, boolean selected) {
        Palette pp = p();
        TextView v = text(t, 14, selected ? pp.onPrimary : pp.text, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(18), dp(11), dp(18), dp(11));
        v.setBackground(shapeStroke(selected ? pp.primary : pp.surface, 28, selected ? pp.primary : pp.stroke));
        return v;
    }

    private void applyBackground() {
        Palette pp = p();
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{pp.bgTop, pp.bgBottom});
        root.setBackground(bg);
    }

    private LinearLayout baseVertical() {
        root.removeAllViews();
        applyBackground();
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(20), dp(18), dp(20), dp(14));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));
        return main;
    }

    private void showHome() {
        currentMode = MODE_HOME;
        Palette pp = p();
        LinearLayout main = baseVertical();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text("SW Launcher", 22, pp.text, Typeface.BOLD));
        titleBox.addView(text("Material 3 Expressive • Workspace", 12, pp.sub, 0));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView styleBtn = pill("Style", false);
        styleBtn.setOnClickListener(v -> showStyleDialog());
        top.addView(styleBtn, new LinearLayout.LayoutParams(dp(112), dp(54)));
        Space spc = new Space(this);
        top.addView(spc, new LinearLayout.LayoutParams(dp(10), 1));
        TextView gear = pill("⚙", false);
        gear.setTextSize(22);
        gear.setOnClickListener(v -> showSettings());
        top.addView(gear, new LinearLayout.LayoutParams(dp(58), dp(54)));
        main.addView(top);

        LinearLayout glance = new LinearLayout(this);
        glance.setOrientation(LinearLayout.VERTICAL);
        glance.setPadding(dp(24), dp(22), dp(24), dp(18));
        glance.setBackground(shapeStroke(argb(pp.surface, 218), 32, pp.stroke));
        TextView time = text(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), 42, pp.text, Typeface.BOLD);
        glance.addView(time);
        glance.addView(text(new SimpleDateFormat("EEE, dd MMM", new Locale("pt", "BR")).format(new Date()), 24, pp.text, Typeface.BOLD));
        glance.addView(text("Área de trabalho • toque para abrir • segure para editar", 13, pp.sub, 0));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, -2);
        glp.setMargins(0, dp(20), 0, dp(16));
        main.addView(glance, glp);

        LinearLayout workspaceHeader = new LinearLayout(this);
        workspaceHeader.setGravity(Gravity.CENTER_VERTICAL);
        workspaceHeader.addView(text("Área de trabalho", 18, pp.text, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = pill("+ Apps", false);
        add.setOnClickListener(v -> showDrawer());
        workspaceHeader.addView(add);
        main.addView(workspaceHeader);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        main.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        List<String> keys = split(sp.getString("workspace", ""));
        if (keys.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(40), dp(24), dp(40));
            empty.setBackground(shapeStroke(argb(pp.surface, 210), 32, pp.stroke));
            empty.addView(text("Sua área está vazia", 22, pp.text, Typeface.BOLD));
            empty.addView(text("Abra a gaveta e segure um app para fixar aqui.", 14, pp.sub, 0));
            TextView go = pill("Abrir gaveta", true);
            go.setOnClickListener(v -> showDrawer());
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(56));
            gp.setMargins(0, dp(18), 0, 0);
            empty.addView(go, gp);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
            ep.setMargins(0, dp(18), 0, 0);
            content.addView(empty, ep);
        } else {
            addGrid(content, keys, columns, true);
        }

        addDock(main);
    }

    private void addDock(LinearLayout main) {
        Palette pp = p();
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setPadding(dp(12), dp(8), dp(12), dp(8));
        dock.setBackground(shapeStroke(argb(pp.surface, 235), 32, pp.stroke));
        List<String> dockKeys = split(sp.getString("dock", ""));
        for (int i = 0; i < Math.min(2, dockKeys.size()); i++) {
            AppEntry e = findByKey(dockKeys.get(i));
            if (e != null) dock.addView(appIcon(e, true), new LinearLayout.LayoutParams(0, -2, 1));
        }
        TextView drawer = pill("•••", true);
        drawer.setTextSize(24);
        drawer.setOnClickListener(v -> showDrawer());
        dock.addView(drawer, new LinearLayout.LayoutParams(0, dp(64), 1));
        for (int i = 2; i < Math.min(4, dockKeys.size()); i++) {
            AppEntry e = findByKey(dockKeys.get(i));
            if (e != null) dock.addView(appIcon(e, true), new LinearLayout.LayoutParams(0, -2, 1));
        }
        LinearLayout.LayoutParams dpv = new LinearLayout.LayoutParams(-1, dp(82));
        dpv.setMargins(0, dp(12), 0, 0);
        main.addView(dock, dpv);
    }

    private void addGrid(LinearLayout parent, List<String> keys, int cols, boolean workspace) {
        LinearLayout row = null;
        int index = 0;
        for (String key : keys) {
            AppEntry e = findByKey(key);
            if (e == null) continue;
            if (index % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                rp.setMargins(0, dp(14), 0, dp(4));
                parent.addView(row, rp);
            }
            View item = appIcon(e, false);
            row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
            index++;
        }
        if (row != null) {
            int remainder = index % cols;
            if (remainder != 0) {
                for (int i = remainder; i < cols; i++) row.addView(new Space(this), new LinearLayout.LayoutParams(0, 1, 1));
            }
        }
    }

    private View appIcon(AppEntry e, boolean dock) {
        Palette pp = p();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4), dp(8), dp(4), dp(6));
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(e.icon);
        int size = dp(dock ? iconSize : iconSize + 4);
        box.addView(icon, new LinearLayout.LayoutParams(size, size));
        TextView label = text(customLabel(e), dock ? 11 : 12, pp.text, 0);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, 0);
        box.addView(label, lp);
        box.setOnClickListener(v -> openApp(e));
        box.setOnLongClickListener(v -> { showAppMenu(e); return true; });
        return box;
    }

    private void showDrawer() {
        currentMode = MODE_DRAWER;
        drawerQuery = "";
        renderDrawer();
    }

    private void renderDrawer() {
        Palette pp = p();
        LinearLayout main = baseVertical();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = pill("‹", false);
        back.setTextSize(28);
        back.setOnClickListener(v -> showHome());
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.setPadding(dp(12), 0, 0, 0);
        title.addView(text("Gaveta de apps", 24, pp.text, Typeface.BOLD));
        title.addView(text("Busca, categorias e apps ocultos", 12, pp.sub, 0));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        main.addView(header);

        EditText search = new EditText(this);
        search.setText(drawerQuery);
        search.setHint("Pesquisar apps");
        search.setSingleLine(true);
        search.setTextColor(pp.text);
        search.setHintTextColor(pp.sub);
        search.setTextSize(17);
        search.setPadding(dp(22), 0, dp(22), 0);
        search.setBackground(shapeStroke(argb(pp.surface, 235), 28, pp.stroke));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(62));
        sp.setMargins(0, dp(18), 0, dp(12));
        main.addView(search, sp);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        String[] cats = {"Todos", "Jogos", "Social", "Mídia", "Ferramentas", "Sistema"};
        for (String cat : cats) {
            TextView chip = pill(cat, cat.equals("Todos"));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(48));
            cp.setMargins(0, 0, dp(8), 0);
            chips.addView(chip, cp);
        }
        hsv.addView(chips);
        main.addView(hsv);

        FrameLayout listFrame = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        listFrame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        main.addView(listFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        fillDrawerGrid(content);
        addAlphabetRail(listFrame);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                drawerQuery = s.toString();
                content.removeAllViews();
                fillDrawerGrid(content);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fillDrawerGrid(LinearLayout content) {
        Set<String> hidden = hiddenSet();
        List<String> keys = new ArrayList<>();
        String q = drawerQuery == null ? "" : drawerQuery.trim().toLowerCase(Locale.ROOT);
        for (AppEntry e : allApps) {
            if (hidden.contains(e.key())) continue;
            if (!q.isEmpty() && !customLabel(e).toLowerCase(Locale.ROOT).contains(q)) continue;
            keys.add(e.key());
        }
        if (keys.isEmpty()) {
            Palette pp = p();
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(22), dp(54), dp(22), dp(54));
            empty.addView(text("Nada encontrado", 22, pp.text, Typeface.BOLD));
            empty.addView(text("Tente outro termo ou revise apps ocultos.", 14, pp.sub, 0));
            content.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        } else {
            addGrid(content, keys, columns, false);
        }
    }

    private void addAlphabetRail(FrameLayout listFrame) {
        Palette pp = p();
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER);
        rail.setPadding(dp(4), dp(8), dp(4), dp(8));
        rail.setBackground(shapeStroke(argb(pp.surface, 200), 22, pp.stroke));
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < letters.length(); i++) {
            char ch = letters.charAt(i);
            TextView t = text(String.valueOf(ch), 10, pp.sub, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            rail.addView(t, new LinearLayout.LayoutParams(dp(28), 0, 1));
            t.setOnClickListener(v -> Toast.makeText(this, "Letra " + ((TextView)v).getText(), Toast.LENGTH_SHORT).show());
        }
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(dp(36), -1, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        rp.setMargins(0, dp(20), 0, dp(20));
        listFrame.addView(rail, rp);
    }

    private void showSettings() {
        currentMode = MODE_SETTINGS;
        Palette pp = p();
        LinearLayout main = baseVertical();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = pill("‹", false);
        back.setTextSize(28);
        back.setOnClickListener(v -> showHome());
        header.addView(back, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.setPadding(dp(12), 0, 0, 0);
        title.addView(text("Configurações", 28, pp.text, Typeface.BOLD));
        title.addView(text("Visual limpo, gaveta, dock e área de trabalho", 13, pp.sub, 0));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        main.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        main.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        settingsRow(list, "Tela inicial", "Grade, apps fixados e layout da área", () -> cycleColumns());
        settingsRow(list, "Gaveta de apps", "Busca, rolagem, categorias e apps ocultos", () -> showDrawer());
        settingsRow(list, "Dock", "Apps fixos no rodapé e botão da gaveta", () -> Toast.makeText(this, "Segure um app para adicionar ou remover do dock.", Toast.LENGTH_LONG).show());
        settingsRow(list, "Aparência", "Neutral, Soft, Bright, Bold e AMOLED", () -> showStyleDialog());
        settingsRow(list, "Tamanho dos ícones", "Atual: " + iconSize + "dp", () -> cycleIconSize());
        settingsRow(list, "Permissão de uso real", "Para ranking real de apps usados", () -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        settingsRow(list, "Recarregar apps", allApps.size() + " apps instalados", () -> { loadApps(); Toast.makeText(this, "Apps recarregados", Toast.LENGTH_SHORT).show(); showSettings(); });
        settingsRow(list, "Resetar layout", "Limpa área, dock e nomes editados", () -> resetLayout());
        settingsRow(list, "Sobre a v0.6", "Workspace + Drawer inspirado em launcher premium", () -> Toast.makeText(this, "SW Launcher v0.6 • M3E Workspace", Toast.LENGTH_LONG).show());
    }

    private void settingsRow(LinearLayout list, String title, String sub, final Runnable action) {
        Palette pp = p();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        card.setBackground(shapeStroke(argb(pp.surface, 235), 28, pp.stroke));
        card.addView(text(title, 18, pp.text, Typeface.BOLD));
        card.addView(text(sub, 13, pp.sub, 0));
        card.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(14), 0, 0);
        list.addView(card, cp);
    }

    private void showStyleDialog() {
        Palette pp = p();
        final Dialog d = sheetDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Style", 28, pp.text, Typeface.BOLD));
        box.addView(text("Paletas inspiradas no Android 17", 14, pp.sub, 0));
        addDivider(box);
        String[] names = {"Neutral", "Soft", "Bright", "Bold", "AMOLED"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView row = bigOption((style == i ? "✓ " : "") + names[i]);
            row.setOnClickListener(v -> {
                style = idx;
                sp.edit().putInt("style", style).apply();
                d.dismiss();
                refreshCurrent();
            });
            box.addView(row);
        }
        d.setContentView(box);
        d.show();
    }

    private Dialog sheetDialog() {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = d.getWindow();
        return d;
    }

    private LinearLayout dialogBox() {
        Palette pp = p();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.setBackground(shapeStroke(pp.surface, 32, pp.stroke));
        int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.86f);
        box.setMinimumWidth(width);
        return box;
    }

    private void setupDialogWindow(Dialog d) {
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            w.setAttributes(lp);
        }
    }

    private void addDivider(LinearLayout box) {
        Palette pp = p();
        View line = new View(this);
        line.setBackgroundColor(pp.stroke);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(18), 0, dp(16));
        box.addView(line, lp);
    }

    private TextView bigOption(String title) {
        Palette pp = p();
        TextView t = text(title, 17, pp.text, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(20), 0, dp(20), 0);
        t.setBackground(shapeStroke(pp.surface2, 26, pp.stroke));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(62));
        lp.setMargins(0, 0, 0, dp(10));
        t.setLayoutParams(lp);
        return t;
    }

    private void showAppMenu(AppEntry e) {
        Palette pp = p();
        Dialog d = sheetDialog();
        LinearLayout box = dialogBox();

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(e.icon);
        head.addView(icon, new LinearLayout.LayoutParams(dp(62), dp(62)));
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(dp(16), 0, 0, 0);
        ht.addView(text(customLabel(e), 22, pp.text, Typeface.BOLD));
        ht.addView(text(e.pkg, 12, pp.sub, 0));
        head.addView(ht, new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(head);
        addDivider(box);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(smallAction("Abrir", () -> { d.dismiss(); openApp(e); }), new LinearLayout.LayoutParams(0, dp(58), 1));
        actions.addView(smallAction(inList("workspace", e.key()) ? "Remover" : "Fixar", () -> { d.dismiss(); toggleList("workspace", e.key(), 24); refreshCurrent(); }), new LinearLayout.LayoutParams(0, dp(58), 1));
        actions.addView(smallAction("Info", () -> { d.dismiss(); openAppInfo(e); }), new LinearLayout.LayoutParams(0, dp(58), 1));
        box.addView(actions);

        box.addView(bigMenu("Editar nome exibido", () -> { d.dismiss(); renameApp(e); }));
        box.addView(bigMenu(inList("dock", e.key()) ? "Remover do dock" : "Adicionar ao dock", () -> { d.dismiss(); toggleList("dock", e.key(), 4); refreshCurrent(); }));
        box.addView(bigMenu("Mover na área de trabalho", () -> { d.dismiss(); moveWorkspace(e); }));
        box.addView(bigMenu(hiddenSet().contains(e.key()) ? "Mostrar app" : "Ocultar da gaveta", () -> { d.dismiss(); toggleHidden(e); refreshCurrent(); }));
        box.addView(bigMenu("Cache e dados", () -> { d.dismiss(); openAppInfo(e); }));
        box.addView(bigMenu("Desinstalar", () -> { d.dismiss(); uninstall(e); }));

        d.setContentView(box);
        d.setOnShowListener(di -> setupDialogWindow(d));
        d.show();
    }

    private TextView smallAction(String t, final Runnable r) {
        Palette pp = p();
        TextView v = text(t, 14, pp.text, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setBackground(shapeStroke(pp.surface2, 24, pp.stroke));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1);
        lp.setMargins(dp(4), 0, dp(4), dp(12));
        v.setLayoutParams(lp);
        v.setOnClickListener(view -> r.run());
        return v;
    }

    private TextView bigMenu(String t, final Runnable r) {
        TextView v = bigOption(t);
        v.setOnClickListener(view -> r.run());
        return v;
    }

    private boolean inList(String key, String value) {
        return split(sp.getString(key, "")).contains(value);
    }

    private void toggleList(String key, String value, int max) {
        List<String> list = split(sp.getString(key, ""));
        if (list.contains(value)) {
            list.remove(value);
        } else {
            if (list.size() >= max) list.remove(0);
            list.add(value);
        }
        putList(key, list);
    }

    private void moveWorkspace(AppEntry e) {
        List<String> list = split(sp.getString("workspace", ""));
        int idx = list.indexOf(e.key());
        if (idx < 0) {
            Toast.makeText(this, "Fixe o app primeiro na área.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idx > 0) {
            Collections.swap(list, idx, idx - 1);
        } else if (list.size() > 1) {
            Collections.swap(list, idx, idx + 1);
        }
        putList("workspace", list);
        Toast.makeText(this, "Movido na área", Toast.LENGTH_SHORT).show();
    }

    private void toggleHidden(AppEntry e) {
        Set<String> set = hiddenSet();
        if (set.contains(e.key())) set.remove(e.key()); else set.add(e.key());
        putList("hidden", new ArrayList<>(set));
    }

    private void renameApp(AppEntry e) {
        final EditText input = new EditText(this);
        input.setText(customLabel(e));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Nome exibido")
                .setView(input)
                .setPositiveButton("Salvar", (d, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = e.label;
                    sp.edit().putString("name_" + e.key(), name).apply();
                    refreshCurrent();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cycleColumns() {
        columns++;
        if (columns > 5) columns = 3;
        sp.edit().putInt("columns", columns).apply();
        Toast.makeText(this, "Colunas: " + columns, Toast.LENGTH_SHORT).show();
        showSettings();
    }

    private void cycleIconSize() {
        iconSize += 4;
        if (iconSize > 72) iconSize = 48;
        sp.edit().putInt("iconSize", iconSize).apply();
        Toast.makeText(this, "Ícones: " + iconSize + "dp", Toast.LENGTH_SHORT).show();
        showSettings();
    }

    private void resetLayout() {
        sp.edit().clear().apply();
        style = 1;
        columns = 4;
        iconSize = 58;
        ensureDefaultLayout();
        Toast.makeText(this, "Layout resetado", Toast.LENGTH_SHORT).show();
        showHome();
    }

    private int argb(int rgb, int alpha) {
        return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }

    private void openApp(AppEntry e) {
        try {
            startActivity(e.launchIntent);
            rememberRecent(e);
        } catch (Exception ex) {
            Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show();
        }
    }

    private void rememberRecent(AppEntry e) {
        List<String> recent = split(sp.getString("recent", ""));
        recent.remove(e.key());
        recent.add(0, e.key());
        while (recent.size() > 30) recent.remove(recent.size() - 1);
        putList("recent", recent);
    }

    private void openAppInfo(AppEntry e) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + e.pkg));
        startActivity(intent);
    }

    private void uninstall(AppEntry e) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + e.pkg));
        startActivity(intent);
    }

    private void refreshCurrent() {
        if (MODE_DRAWER.equals(currentMode)) renderDrawer();
        else if (MODE_SETTINGS.equals(currentMode)) showSettings();
        else showHome();
    }

    @Override
    public void onBackPressed() {
        if (!MODE_HOME.equals(currentMode)) showHome(); else super.onBackPressed();
    }
}
