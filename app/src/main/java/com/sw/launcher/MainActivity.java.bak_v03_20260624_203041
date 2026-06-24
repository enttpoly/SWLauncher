package com.sw.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREFS = "sw_launcher_prefs";
    private static final String MODE_HOME = "home";
    private static final String MODE_ALL = "all";
    private static final String MODE_RECENT = "recent";
    private static final String MODE_USED = "used";
    private static final String MODE_HIDDEN = "hidden";

    private static final String CAT_ALL = "all";
    private static final String CAT_GAMES = "games";
    private static final String CAT_SOCIAL = "social";
    private static final String CAT_MEDIA = "media";
    private static final String CAT_TOOLS = "tools";
    private static final String CAT_SYSTEM = "system";

    private final ArrayList<AppInfo> allApps = new ArrayList<>();
    private final ArrayList<AppInfo> visibleApps = new ArrayList<>();
    private final ArrayList<Button> modeButtons = new ArrayList<>();
    private final ArrayList<Button> categoryButtons = new ArrayList<>();

    private SharedPreferences prefs;
    private AppsAdapter adapter;
    private GridView grid;
    private FrameLayout rootFrame;
    private LinearLayout root;
    private LinearLayout alphabetRail;
    private TextView clockText;
    private TextView subtitleText;
    private TextView statusText;
    private TextView emptyView;
    private EditText searchBox;

    private String currentMode = MODE_HOME;
    private String currentCategory = CAT_ALL;
    private String currentSearch = "";
    private int columns = 4;
    private int globalIconDp = 56;
    private int theme = 0;
    private boolean showHiddenInAll = false;
    private String turboMode = "Balanced";

    private final Handler clockHandler = new Handler(Looper.getMainLooper());

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadLauncherSettings();
        buildHomeUi();
        loadInstalledApps();
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInstalledApps();
        updateClock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockRunnable);
    }

    private void loadLauncherSettings() {
        columns = prefs.getInt("columns", 4);
        globalIconDp = prefs.getInt("global_icon_dp", 56);
        theme = prefs.getInt("theme", 0);
        showHiddenInAll = prefs.getBoolean("show_hidden_in_all", false);
        currentMode = prefs.getString("last_mode", MODE_HOME);
        currentCategory = prefs.getString("last_category", CAT_ALL);
        turboMode = prefs.getString("turbo_mode", "Balanced");
    }

    private void saveLauncherSettings() {
        prefs.edit()
                .putInt("columns", columns)
                .putInt("global_icon_dp", globalIconDp)
                .putInt("theme", theme)
                .putBoolean("show_hidden_in_all", showHiddenInAll)
                .putString("last_mode", currentMode)
                .putString("last_category", currentCategory)
                .putString("turbo_mode", turboMode)
                .apply();
    }

    private void buildHomeUi() {
        modeButtons.clear();
        categoryButtons.clear();

        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(bgColor());
        rootFrame.addView(new CircuitBackgroundView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(12), dp(10));
        rootFrame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("SW Launcher");
        title.setTextColor(textColor());
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        TextView micro = new TextView(this);
        micro.setText("SW CORE UI  •  " + themeName().toUpperCase(Locale.getDefault()));
        micro.setTextColor(mutedColor());
        micro.setTextSize(9);
        micro.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        titleBox.addView(title);
        titleBox.addView(micro);
        topRow.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button turboButton = pillButton("⚡ " + shortTurboName(), 42, true);
        turboButton.setTextSize(12);
        turboButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openTurboPanel(); }
        });
        LinearLayout.LayoutParams turboLp = new LinearLayout.LayoutParams(dp(92), dp(42));
        turboLp.setMargins(0, 0, dp(8), 0);
        topRow.addView(turboButton, turboLp);

        Button settingsButton = pillButton("⚙", 42, false);
        settingsButton.setTextSize(20);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openLauncherSettings(); }
        });
        topRow.addView(settingsButton, new LinearLayout.LayoutParams(dp(48), dp(42)));
        root.addView(topRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(16), dp(14), dp(16), dp(14));
        hero.setBackground(roundGradientDrawable(cardColor(), cardAltColor(), dp(24), accentSoft(), 1));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.setMargins(0, dp(14), 0, dp(10));

        clockText = new TextView(this);
        clockText.setTextColor(textColor());
        clockText.setTextSize(35);
        clockText.setTypeface(Typeface.DEFAULT_BOLD);
        clockText.setGravity(Gravity.START);
        hero.addView(clockText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusText = new TextView(this);
        statusText.setTextColor(accentColor());
        statusText.setTextSize(11);
        statusText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        statusText.setPadding(0, dp(4), 0, 0);
        hero.addView(statusText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(hero, heroLp);

        subtitleText = new TextView(this);
        subtitleText.setTextColor(mutedColor());
        subtitleText.setTextSize(12);
        subtitleText.setPadding(dp(2), 0, 0, dp(8));
        root.addView(subtitleText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Pesquisar apps, jogos e ferramentas...");
        searchBox.setHintTextColor(Color.argb(145, 255, 255, 255));
        searchBox.setTextColor(textColor());
        searchBox.setTextSize(14);
        searchBox.setPadding(dp(16), 0, dp(16), 0);
        searchBox.setInputType(InputType.TYPE_CLASS_TEXT);
        searchBox.setBackground(roundDrawable(searchColor(), dp(18), accentSoft(), 1));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(searchBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER);
        modeRow.setPadding(0, dp(10), 0, dp(6));
        addModeButton(modeRow, "Início", MODE_HOME);
        addModeButton(modeRow, "Apps", MODE_ALL);
        addModeButton(modeRow, "Recentes", MODE_RECENT);
        addModeButton(modeRow, "Usados", MODE_USED);
        root.addView(modeRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        HorizontalCategoryRow categoryRow = new HorizontalCategoryRow(this);
        addCategoryButton(categoryRow.inner, "Todos", CAT_ALL);
        addCategoryButton(categoryRow.inner, "Jogos", CAT_GAMES);
        addCategoryButton(categoryRow.inner, "Social", CAT_SOCIAL);
        addCategoryButton(categoryRow.inner, "Mídia", CAT_MEDIA);
        addCategoryButton(categoryRow.inner, "Ferramentas", CAT_TOOLS);
        addCategoryButton(categoryRow.inner, "Sistema", CAT_SYSTEM);
        root.addView(categoryRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        LinearLayout contentRow = new LinearLayout(this);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);

        FrameLayout gridFrame = new FrameLayout(this);
        grid = new GridView(this);
        grid.setNumColumns(columns);
        grid.setVerticalSpacing(dp(13));
        grid.setHorizontalSpacing(dp(8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setGravity(Gravity.CENTER);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(8), 0, dp(82));
        grid.setSelector(android.R.color.transparent);

        emptyView = new TextView(this);
        emptyView.setTextColor(mutedColor());
        emptyView.setTextSize(14);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(28), dp(28), dp(28), dp(28));
        emptyView.setBackground(roundDrawable(Color.argb(28, 255, 255, 255), dp(24), accentSoft(), 1));

        gridFrame.addView(grid, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams emptyLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180));
        emptyLp.gravity = Gravity.CENTER;
        emptyLp.setMargins(dp(8), 0, dp(8), 0);
        gridFrame.addView(emptyView, emptyLp);
        grid.setEmptyView(emptyView);

        adapter = new AppsAdapter();
        grid.setAdapter(adapter);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openApp(visibleApps.get(position));
            }
        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                openAppMenu(visibleApps.get(position));
                return true;
            }
        });

        contentRow.addView(gridFrame, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        ScrollView alphaScroll = new ScrollView(this);
        alphaScroll.setFillViewport(false);
        alphaScroll.setBackground(roundDrawable(Color.argb(20, 255, 255, 255), dp(18), Color.argb(22, 255, 255, 255), 1));
        alphabetRail = new LinearLayout(this);
        alphabetRail.setOrientation(LinearLayout.VERTICAL);
        alphabetRail.setGravity(Gravity.CENTER_HORIZONTAL);
        alphaScroll.addView(alphabetRail, new ScrollView.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams alphaLp = new LinearLayout.LayoutParams(dp(32), ViewGroup.LayoutParams.MATCH_PARENT);
        alphaLp.setMargins(dp(5), dp(8), 0, dp(82));
        contentRow.addView(alphaScroll, alphaLp);

        root.addView(contentRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(8), dp(8), dp(8));
        dock.setBackground(roundGradientDrawable(dockColor(), Color.argb(84, Color.red(accentColor()), Color.green(accentColor()), Color.blue(accentColor())), dp(26), accentSoft(), 1));
        addDockButton(dock, "⚙ Config", new View.OnClickListener() {
            @Override public void onClick(View v) { openLauncherSettings(); }
        });
        addDockButton(dock, "⌂ Home", new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(MODE_HOME); }
        });
        addDockButton(dock, "⚡ Turbo", new View.OnClickListener() {
            @Override public void onClick(View v) { openTurboPanel(); }
        });
        addDockButton(dock, "⌕ Busca", new View.OnClickListener() {
            @Override public void onClick(View v) { focusSearch(); }
        });
        LinearLayout.LayoutParams dockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62));
        dockLp.setMargins(0, dp(2), 0, 0);
        root.addView(dock, dockLp);

        setContentView(rootFrame);
        buildAlphabetRail();
        refreshModeButtons();
        refreshCategoryButtons();
        updateClock();
        updateSubtitle();
    }

    private void addModeButton(LinearLayout row, String text, final String mode) {
        Button b = pillButton(text, 38, false);
        b.setTextSize(12);
        b.setTag(mode);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(mode); }
        });
        modeButtons.add(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(b, lp);
    }

    private void addCategoryButton(LinearLayout row, String text, final String category) {
        Button b = pillButton(text, 34, false);
        b.setTextSize(11);
        b.setTag(category);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setCategory(category); }
        });
        categoryButtons.add(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(108), dp(34));
        lp.setMargins(dp(3), 0, dp(5), 0);
        row.addView(b, lp);
    }

    private void addDockButton(LinearLayout row, String text, View.OnClickListener listener) {
        Button b = pillButton(text, 42, false);
        b.setTextSize(11);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(b, lp);
    }

    private Button pillButton(String text, int height, boolean active) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(active ? Color.BLACK : textColor());
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        int fill = active ? accentColor() : Color.argb(48, 255, 255, 255);
        int stroke = active ? accentColor() : Color.argb(40, 255, 255, 255);
        b.setBackground(roundDrawable(fill, dp(height / 2), stroke, 1));
        return b;
    }

    private Button smallActionButton(String text) {
        Button b = pillButton(text, 42, false);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(roundDrawable(Color.argb(48, Color.red(accentColor()), Color.green(accentColor()), Color.blue(accentColor())), dp(16), accentSoft(), 1));
        return b;
    }

    private void refreshModeButtons() {
        for (Button b : modeButtons) {
            boolean active = currentMode.equals(String.valueOf(b.getTag()));
            b.setTextColor(active ? Color.BLACK : textColor());
            b.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            b.setBackground(roundDrawable(active ? accentColor() : Color.argb(42, 255, 255, 255), dp(19), active ? accentColor() : Color.argb(35, 255, 255, 255), 1));
        }
    }

    private void refreshCategoryButtons() {
        for (Button b : categoryButtons) {
            boolean active = currentCategory.equals(String.valueOf(b.getTag()));
            b.setTextColor(active ? Color.BLACK : textColor());
            b.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            b.setBackground(roundDrawable(active ? accent2Color() : Color.argb(36, 255, 255, 255), dp(17), active ? accent2Color() : Color.argb(32, 255, 255, 255), 1));
        }
    }

    private void updateClock() {
        if (clockText == null) return;
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(new Date());
        clockText.setText(time + "\n" + date);
        if (statusText != null) {
            statusText.setText("GAME CORE: " + turboMode.toUpperCase(Locale.getDefault()) + "  •  " + getBatteryStatusText() + "  •  " + visibleApps.size() + " APPS");
        }
    }

    private String getBatteryStatusText() {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent battery = registerReceiver(null, filter);
            if (battery == null) return "BAT --";
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int pct = 0;
            if (level >= 0 && scale > 0) pct = Math.round(level * 100f / scale);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            return "BAT " + pct + "%" + (charging ? " ⚡" : "");
        } catch (Exception e) {
            return "BAT --";
        }
    }

    private void updateSubtitle() {
        if (subtitleText == null) return;
        String modeName;
        if (MODE_HOME.equals(currentMode)) modeName = "Home gamer: seus apps fixados ficam aqui";
        else if (MODE_RECENT.equals(currentMode)) modeName = "Recentes: apps abertos pelo SW Launcher";
        else if (MODE_USED.equals(currentMode)) modeName = "Usados: ranking criado pelo SW Launcher";
        else if (MODE_HIDDEN.equals(currentMode)) modeName = "Apps ocultos";
        else modeName = "Gaveta gamer com busca, categorias e A-Z";
        subtitleText.setText(modeName + " • segure um app para editar");
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyView == null) return;
        if (MODE_HOME.equals(currentMode)) {
            emptyView.setText("Nenhum app fixado ainda\n\nAbra a aba Apps, segure um app e toque em Fixar na Home.");
        } else if (MODE_RECENT.equals(currentMode)) {
            emptyView.setText("Sem apps recentes ainda\n\nAbra alguns apps pelo SW Launcher para alimentar esta área.");
        } else if (MODE_USED.equals(currentMode)) {
            emptyView.setText("Sem ranking de uso ainda\n\nO ranking aparece depois que você abrir apps pelo launcher.");
        } else if (MODE_HIDDEN.equals(currentMode)) {
            emptyView.setText("Nenhum app oculto\n\nVocê pode ocultar apps segurando um ícone.");
        } else {
            emptyView.setText("Nada encontrado\n\nTente limpar a busca ou trocar o filtro.");
        }
    }

    private void setMode(String mode) {
        currentMode = mode;
        saveLauncherSettings();
        refreshModeButtons();
        updateSubtitle();
        applyFilters();
    }

    private void setCategory(String category) {
        currentCategory = category;
        if (MODE_HOME.equals(currentMode)) currentMode = MODE_ALL;
        saveLauncherSettings();
        refreshModeButtons();
        refreshCategoryButtons();
        updateSubtitle();
        applyFilters();
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);

        allApps.clear();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : resolved) {
            String packageName = info.activityInfo.packageName;
            if (packageName == null || packageName.equals(getPackageName())) continue;
            if (seen.contains(packageName)) continue;
            seen.add(packageName);

            String originalLabel = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            AppInfo app = new AppInfo(originalLabel, packageName, icon);
            app.category = guessCategory(originalLabel, packageName);
            readAppPrefs(app);
            allApps.add(app);
        }
        applyFilters();
    }

    private String guessCategory(String label, String pkg) {
        String text = (label + " " + pkg).toLowerCase(Locale.getDefault());
        if (hasAny(text, new String[]{"roblox", "brawl", "game", "gaming", "play games", "steam", "xbox", "nintendo", "epic", "riot", "minecraft", "free fire", "pubg", "cod", "fortnite"})) return CAT_GAMES;
        if (hasAny(text, new String[]{"whatsapp", "discord", "instagram", "facebook", "messenger", "telegram", "twitter", "x", "kick", "tiktok", "threads", "snap", "gmail", "email"})) return CAT_SOCIAL;
        if (hasAny(text, new String[]{"youtube", "spotify", "music", "musicolet", "gallery", "galeria", "camera", "câmera", "photos", "fotos", "capcut", "inshot", "bandlab", "player", "video", "gravador", "recorder"})) return CAT_MEDIA;
        if (hasAny(text, new String[]{"settings", "config", "calculator", "calculadora", "files", "arquivos", "authenticator", "installer", "keyboard", "gboard", "maps", "chrome", "browser", "store", "play store"})) return CAT_TOOLS;
        if (pkg.startsWith("com.android") || pkg.startsWith("com.samsung") || pkg.startsWith("com.google.android")) return CAT_SYSTEM;
        return CAT_ALL;
    }

    private boolean hasAny(String text, String[] needles) {
        for (String n : needles) if (text.contains(n)) return true;
        return false;
    }

    private void readAppPrefs(AppInfo app) {
        app.alias = prefs.getString("alias_" + app.packageName, "");
        app.favorite = prefs.getBoolean("fav_" + app.packageName, false);
        app.hidden = prefs.getBoolean("hidden_" + app.packageName, false);
        app.lastOpened = prefs.getLong("last_" + app.packageName, 0L);
        app.openCount = prefs.getInt("count_" + app.packageName, 0);
        app.sizeMode = prefs.getInt("size_" + app.packageName, 0);
    }

    private void applyFilters() {
        visibleApps.clear();
        String q = currentSearch == null ? "" : currentSearch.trim().toLowerCase(Locale.getDefault());

        for (AppInfo app : allApps) {
            if (MODE_HOME.equals(currentMode) && !app.favorite) continue;
            if (MODE_HIDDEN.equals(currentMode) && !app.hidden) continue;
            if (!MODE_HIDDEN.equals(currentMode) && app.hidden && !showHiddenInAll) continue;
            if (MODE_RECENT.equals(currentMode) && app.lastOpened <= 0) continue;
            if (MODE_USED.equals(currentMode) && app.openCount <= 0) continue;
            if (!CAT_ALL.equals(currentCategory) && !MODE_HOME.equals(currentMode) && !currentCategory.equals(app.category)) continue;

            String name = app.displayName().toLowerCase(Locale.getDefault());
            String pkg = app.packageName.toLowerCase(Locale.getDefault());
            if (!q.isEmpty() && !name.contains(q) && !pkg.contains(q)) continue;
            visibleApps.add(app);
        }

        if (MODE_HOME.equals(currentMode)) {
            sortByHomeOrder(visibleApps);
        } else if (MODE_RECENT.equals(currentMode)) {
            Collections.sort(visibleApps, new Comparator<AppInfo>() {
                @Override public int compare(AppInfo a, AppInfo b) { return Long.compare(b.lastOpened, a.lastOpened); }
            });
        } else if (MODE_USED.equals(currentMode)) {
            Collections.sort(visibleApps, new Comparator<AppInfo>() {
                @Override public int compare(AppInfo a, AppInfo b) { return Integer.compare(b.openCount, a.openCount); }
            });
        } else {
            Collections.sort(visibleApps, new Comparator<AppInfo>() {
                @Override public int compare(AppInfo a, AppInfo b) { return a.displayName().compareToIgnoreCase(b.displayName()); }
            });
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmptyState();
        updateClock();
    }

    private void sortByHomeOrder(ArrayList<AppInfo> list) {
        final ArrayList<String> order = getHomeOrder();
        Collections.sort(list, new Comparator<AppInfo>() {
            @Override public int compare(AppInfo a, AppInfo b) {
                int ia = order.indexOf(a.packageName);
                int ib = order.indexOf(b.packageName);
                if (ia < 0) ia = 9999;
                if (ib < 0) ib = 9999;
                if (ia != ib) return Integer.compare(ia, ib);
                return a.displayName().compareToIgnoreCase(b.displayName());
            }
        });
    }

    private void openApp(AppInfo app) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launchIntent == null) {
            Toast.makeText(this, "Não consegui abrir: " + app.displayName(), Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit()
                .putLong("last_" + app.packageName, System.currentTimeMillis())
                .putInt("count_" + app.packageName, app.openCount + 1)
                .apply();
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
    }

    private void openAppMenu(final AppInfo app) {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout box = panelBox();

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        head.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(12), 0, 0, 0);
        TextView name = panelTitle(app.displayName());
        TextView pkg = panelSmall(app.packageName);
        texts.addView(name);
        texts.addView(pkg);
        head.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(head);

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setPadding(0, dp(14), 0, dp(8));
        addQuickAppAction(quick, "Abrir", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openApp(app); }});
        addQuickAppAction(quick, app.favorite ? "Soltar" : "Fixar", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); toggleFavorite(app); }});
        addQuickAppAction(quick, "Info", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openAppDetails(app); }});
        addQuickAppAction(quick, "Cache", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openAppDetails(app); }});
        box.addView(quick);

        addPanelRow(box, "✎ Editar nome", "Muda o nome só dentro do SW Launcher", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); renameApp(app); }});
        addPanelRow(box, "◈ Tamanho do ícone", "Pequeno, normal, grande ou gigante", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); chooseAppSize(app); }});
        addPanelRow(box, "▣ Trocar ícone", "Preparado para a v0.4", new View.OnClickListener() { @Override public void onClick(View v) { Toast.makeText(MainActivity.this, "Troca real de ícone entra na v0.4", Toast.LENGTH_SHORT).show(); }});
        if (app.favorite) {
            addPanelRow(box, "← Mover para esquerda/cima", "Reorganiza na Home", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); moveFavorite(app, -1); }});
            addPanelRow(box, "→ Mover para direita/baixo", "Reorganiza na Home", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); moveFavorite(app, 1); }});
        }
        addPanelRow(box, app.hidden ? "◎ Mostrar app" : "◌ Ocultar app", app.hidden ? "Volta para a gaveta" : "Remove da lista principal", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); toggleHidden(app); }});
        addPanelRow(box, "▤ Abrir na Play Store", "Página do app, quando disponível", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openPlayStore(app); }});
        addPanelRow(box, "↺ Resetar edição", "Remove nome, tamanho, fixo e oculto", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); resetAppCustomization(app); }});

        dialog.setView(box);
        dialog.show();
        styleDialog(dialog);
    }

    private void addQuickAppAction(LinearLayout row, String text, View.OnClickListener listener) {
        Button b = smallActionButton(text);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(b, lp);
    }

    private void renameApp(final AppInfo app) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(app.displayName());
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setTextColor(Color.BLACK);

        new AlertDialog.Builder(this)
                .setTitle("Editar nome")
                .setMessage("Esse nome muda só dentro do SW Launcher.")
                .setView(input)
                .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String text = input.getText().toString().trim();
                        prefs.edit().putString("alias_" + app.packageName, text).apply();
                        loadInstalledApps();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void chooseAppSize(final AppInfo app) {
        final String[] sizes = {"Pequeno", "Normal", "Grande", "Gigante"};
        int checked = app.sizeMode + 1;
        if (checked < 0) checked = 1;
        if (checked > 3) checked = 3;
        new AlertDialog.Builder(this)
                .setTitle("Tamanho do ícone")
                .setSingleChoiceItems(sizes, checked, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        int value = which - 1;
                        prefs.edit().putInt("size_" + app.packageName, value).apply();
                        dialog.dismiss();
                        loadInstalledApps();
                    }
                })
                .show();
    }

    private void toggleFavorite(AppInfo app) {
        boolean newValue = !app.favorite;
        prefs.edit().putBoolean("fav_" + app.packageName, newValue).apply();
        ArrayList<String> order = getHomeOrder();
        if (newValue && !order.contains(app.packageName)) order.add(app.packageName);
        if (!newValue) order.remove(app.packageName);
        saveHomeOrder(order);
        Toast.makeText(this, newValue ? "Fixado na Home" : "Removido da Home", Toast.LENGTH_SHORT).show();
        loadInstalledApps();
    }

    private void moveFavorite(AppInfo app, int direction) {
        ArrayList<String> order = getHomeOrder();
        if (!order.contains(app.packageName)) order.add(app.packageName);
        int index = order.indexOf(app.packageName);
        int newIndex = index + direction;
        if (newIndex < 0 || newIndex >= order.size()) {
            Toast.makeText(this, "Já está no limite", Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.swap(order, index, newIndex);
        saveHomeOrder(order);
        loadInstalledApps();
    }

    private void toggleHidden(AppInfo app) {
        boolean newValue = !app.hidden;
        prefs.edit().putBoolean("hidden_" + app.packageName, newValue).apply();
        Toast.makeText(this, newValue ? "App ocultado" : "App voltou para a lista", Toast.LENGTH_SHORT).show();
        loadInstalledApps();
    }

    private void resetAppCustomization(AppInfo app) {
        prefs.edit()
                .remove("alias_" + app.packageName)
                .remove("size_" + app.packageName)
                .remove("hidden_" + app.packageName)
                .remove("fav_" + app.packageName)
                .apply();
        ArrayList<String> order = getHomeOrder();
        order.remove(app.packageName);
        saveHomeOrder(order);
        loadInstalledApps();
    }

    private void openAppDetails(AppInfo app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + app.packageName));
        safeStart(intent);
    }

    private void openPlayStore(AppInfo app) {
        try {
            Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app.packageName));
            startActivity(market);
        } catch (Exception e) {
            safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app.packageName)));
        }
    }

    private void openCacheOptions() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout box = panelBox();
        box.addView(panelTitle("Cache e dados"));
        box.addView(panelSmall("O Android não deixa um launcher comum apagar dados de outros apps sozinho. Esta área abre telas oficiais para limpeza manual."));
        addPanelRow(box, "Armazenamento do Android", "Gerencie espaço do aparelho", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); safeStart(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)); }});
        addPanelRow(box, "Solicitar limpeza geral de cache", "Pode variar por versão do Android", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); requestClearAllCache(); }});
        addPanelRow(box, "Apps instalados", "Abrir lista oficial de apps", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); safeStart(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)); }});
        dialog.setView(box);
        dialog.show();
        styleDialog(dialog);
    }

    private void requestClearAllCache() {
        try {
            Intent intent = new Intent("android.intent.action.CLEAR_APP_CACHE");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Abrindo armazenamento do Android", Toast.LENGTH_SHORT).show();
            safeStart(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        }
    }

    private void openLauncherSettings() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout box = panelBox();
        box.addView(panelTitle("Configurações do SW Launcher"));
        box.addView(panelSmall("SW Core UI • launcher gamer modular"));
        addPanelRow(box, "⌂ Home Screen", "Colunas, fixos e visual da tela inicial", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openHomeSettings(); }});
        addPanelRow(box, "▦ App Drawer", "Grade, tamanho e apps ocultos", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openDrawerSettings(); }});
        addPanelRow(box, "◈ Tema Gamer", themeName() + " • glow e cores", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); chooseTheme(); }});
        addPanelRow(box, "⚡ Turbo Panel", "Balanced, Performance, Turbo e Battery", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openTurboPanel(); }});
        addPanelRow(box, "◌ Apps ocultos", "Ver ou mostrar apps escondidos", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openHiddenSettings(); }});
        addPanelRow(box, "▤ Cache e dados", "Atalhos para telas oficiais do Android", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openCacheOptions(); }});
        addPanelRow(box, "☰ Permissão de uso real", "Para estatísticas futuras do sistema", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); safeStart(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }});
        addPanelRow(box, "↻ Recarregar apps", "Atualiza a lista instalada", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); loadInstalledApps(); Toast.makeText(MainActivity.this, "Apps recarregados", Toast.LENGTH_SHORT).show(); }});
        addPanelRow(box, "▣ Backup e pastas", "Preparado para v0.4/v0.5", new View.OnClickListener() { @Override public void onClick(View v) { Toast.makeText(MainActivity.this, "Pastas e backup entram nas próximas versões", Toast.LENGTH_SHORT).show(); }});
        addPanelRow(box, "↺ Resetar launcher", "Apaga ajustes locais", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); confirmResetLauncher(); }});
        addPanelRow(box, "ⓘ Sobre", "Versão e novidades", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); showAbout(); }});
        dialog.setView(box);
        dialog.show();
        styleDialog(dialog);
    }

    private void openHomeSettings() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout box = panelBox();
        box.addView(panelTitle("Home Screen"));
        addPanelRow(box, "Colunas da grade: " + columns, "Define quantos apps aparecem por linha", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); chooseColumns(); }});
        addPanelRow(box, "Apps fixados", "Use segurar app > Fixar na Home", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); setMode(MODE_HOME); }});
        addPanelRow(box, "Dock gamer", "Ativo nesta versão", new View.OnClickListener() { @Override public void onClick(View v) { Toast.makeText(MainActivity.this, "Controle completo da dock entra na v0.4", Toast.LENGTH_SHORT).show(); }});
        dialog.setView(box);
        dialog.show();
        styleDialog(dialog);
    }

    private void openDrawerSettings() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout box = panelBox();
        box.addView(panelTitle("App Drawer"));
        addPanelRow(box, "Tamanho global: " + globalIconDp + "dp", "Controla os ícones da grade", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); chooseGlobalIconSize(); }});
        addPanelRow(box, "Categorias gamer", "Jogos, Social, Mídia, Ferramentas e Sistema", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); setMode(MODE_ALL); }});
        addPanelRow(box, "Rolagem A-Z", "Use a régua lateral para saltar letras", new View.OnClickListener() { @Override public void onClick(View v) { Toast.makeText(MainActivity.this, "A-Z já está ativo na lateral", Toast.LENGTH_SHORT).show(); }});
        addPanelRow(box, showHiddenInAll ? "Ocultar escondidos na gaveta" : "Mostrar escondidos na gaveta", "Controla apps ocultos na lista principal", new View.OnClickListener() { @Override public void onClick(View v) { showHiddenInAll = !showHiddenInAll; saveLauncherSettings(); dialog.dismiss(); loadInstalledApps(); }});
        dialog.setView(box);
        dialog.show();
        styleDialog(dialog);
    }

    private void openHiddenSettings() {
        final String[] items = {showHiddenInAll ? "Ocultar escondidos na gaveta" : "Mostrar escondidos na gaveta", "Ver apps ocultos"};
        new AlertDialog.Builder(this)
                .setTitle("Apps ocultos")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showHiddenInAll = !showHiddenInAll;
                            saveLauncherSettings();
                            loadInstalledApps();
                        } else {
                            currentMode = MODE_HIDDEN;
                            saveLauncherSettings();
                            refreshModeButtons();
                            updateSubtitle();
                            applyFilters();
                        }
                    }
                }).show();
    }

    private void chooseColumns() {
        final String[] values = {"3 colunas", "4 colunas", "5 colunas", "6 colunas"};
        int checked = columns == 3 ? 0 : columns == 4 ? 1 : columns == 5 ? 2 : 3;
        new AlertDialog.Builder(this)
                .setTitle("Colunas da grade")
                .setSingleChoiceItems(values, checked, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        columns = which + 3;
                        saveLauncherSettings();
                        dialog.dismiss();
                        buildHomeUi();
                        loadInstalledApps();
                    }
                })
                .show();
    }

    private void chooseGlobalIconSize() {
        final String[] labels = {"Pequeno", "Normal", "Grande", "Gigante"};
        final int[] values = {46, 56, 66, 76};
        int checked = 1;
        for (int i = 0; i < values.length; i++) if (values[i] == globalIconDp) checked = i;
        new AlertDialog.Builder(this)
                .setTitle("Tamanho global")
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        globalIconDp = values[which];
                        saveLauncherSettings();
                        dialog.dismiss();
                        loadInstalledApps();
                    }
                })
                .show();
    }

    private void chooseTheme() {
        final String[] labels = {"Cyber Violet", "ROG Red", "Cyber Blue", "Toxic Green", "AMOLED Pure", "Ice Neon"};
        int checked = theme;
        if (checked < 0 || checked >= labels.length) checked = 0;
        new AlertDialog.Builder(this)
                .setTitle("Tema Gamer")
                .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        theme = which;
                        saveLauncherSettings();
                        dialog.dismiss();
                        buildHomeUi();
                        loadInstalledApps();
                    }
                })
                .show();
    }

    private void openTurboPanel() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout box = panelBox();
        box.addView(panelTitle("SW Turbo Panel"));
        box.addView(panelSmall("Modo atual: " + turboMode + " • atalhos seguros, sem root"));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, dp(8));
        addTurboChoice(row, dialog, "Balanced");
        addTurboChoice(row, dialog, "Performance");
        addTurboChoice(row, dialog, "Turbo");
        addTurboChoice(row, dialog, "Battery");
        box.addView(row);

        addPanelRow(box, "☾ Não perturbe", "Abrir configurações de notificações", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); safeStart(new Intent("android.settings.NOTIFICATION_SETTINGS")); }});
        addPanelRow(box, "☀ Brilho e tela", "Display, taxa e economia", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); safeStart(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); }});
        addPanelRow(box, "▣ Apps recentes", "Voltar para aba Recentes", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); setMode(MODE_RECENT); }});
        addPanelRow(box, "Cache rápido", "Atalhos de limpeza", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openCacheOptions(); }});
        dialog.setView(box);
        dialog.show();
        styleDialog(dialog);
    }

    private void addTurboChoice(LinearLayout row, final AlertDialog dialog, final String mode) {
        Button b = smallActionButton(mode);
        if (mode.equals(turboMode)) {
            b.setTextColor(Color.BLACK);
            b.setBackground(roundDrawable(accentColor(), dp(16), accentColor(), 1));
        }
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                turboMode = mode;
                saveLauncherSettings();
                dialog.dismiss();
                buildHomeUi();
                loadInstalledApps();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        row.addView(b, lp);
    }

    private void confirmResetLauncher() {
        new AlertDialog.Builder(this)
                .setTitle("Resetar launcher?")
                .setMessage("Isso apaga nomes editados, favoritos, apps ocultos, contadores e ajustes visuais.")
                .setPositiveButton("Resetar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().clear().apply();
                        loadLauncherSettings();
                        buildHomeUi();
                        loadInstalledApps();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("SW Launcher v0.3")
                .setMessage("Novidades:\n\n• Redesign gamer Cyber Violet\n• Header com status SW Core\n• Painel Turbo visual\n• Categorias rápidas\n• Menu de app em painel\n• Configurações reorganizadas\n• Dock gamer\n• Estados vazios melhores\n• Barra A-Z integrada\n\nLimite real: cache/dados e performance profunda dependem das telas e permissões do Android.")
                .setPositiveButton("Fechar", null)
                .show();
    }

    private void focusSearch() {
        currentMode = MODE_ALL;
        refreshModeButtons();
        updateSubtitle();
        applyFilters();
        searchBox.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
    }

    private void safeStart(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui abrir essa tela neste aparelho", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildAlphabetRail() {
        if (alphabetRail == null) return;
        alphabetRail.removeAllViews();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < letters.length(); i++) {
            final String letter = String.valueOf(letters.charAt(i));
            TextView tv = new TextView(this);
            tv.setText(letter);
            tv.setTextColor(accent2Color());
            tv.setTextSize(9);
            tv.setGravity(Gravity.CENTER);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { scrollToLetter(letter); }
            });
            alphabetRail.addView(tv, new LinearLayout.LayoutParams(dp(26), dp(19)));
        }
    }

    private void scrollToLetter(String letter) {
        if (MODE_HOME.equals(currentMode)) {
            currentMode = MODE_ALL;
            updateSubtitle();
            saveLauncherSettings();
            refreshModeButtons();
            applyFilters();
        }
        for (int i = 0; i < visibleApps.size(); i++) {
            String name = visibleApps.get(i).displayName().toUpperCase(Locale.getDefault());
            if (name.startsWith(letter)) {
                grid.setSelection(i);
                Toast.makeText(this, letter, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Sem apps com " + letter, Toast.LENGTH_SHORT).show();
    }

    private ArrayList<String> getHomeOrder() {
        String raw = prefs.getString("home_order", "");
        ArrayList<String> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return result;
        String[] parts = raw.split("\\|");
        for (String p : parts) if (p != null && !p.trim().isEmpty()) result.add(p);
        return result;
    }

    private void saveHomeOrder(ArrayList<String> order) {
        StringBuilder sb = new StringBuilder();
        for (String p : order) {
            if (sb.length() > 0) sb.append("|");
            sb.append(p);
        }
        prefs.edit().putString("home_order", sb.toString()).apply();
    }

    private LinearLayout panelBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(18));
        box.setBackground(roundGradientDrawable(cardColor(), cardAltColor(), dp(24), accentSoft(), 1));
        return box;
    }

    private TextView panelTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor());
        tv.setTextSize(20);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private TextView panelSmall(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(mutedColor());
        tv.setTextSize(11);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private void addPanelRow(LinearLayout box, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(roundDrawable(Color.argb(34, 255, 255, 255), dp(16), Color.argb(28, 255, 255, 255), 1));
        row.setOnClickListener(listener);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(textColor());
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(t);

        TextView sub = new TextView(this);
        sub.setText(subtitle);
        sub.setTextColor(mutedColor());
        sub.setTextSize(11);
        sub.setPadding(0, dp(2), 0, 0);
        row.addView(sub);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        box.addView(row, lp);
    }

    private void styleDialog(AlertDialog dialog) {
        try {
            Window w = dialog.getWindow();
            if (w != null) w.setBackgroundDrawableResource(android.R.color.transparent);
        } catch (Exception ignored) {}
    }

    private GradientDrawable roundDrawable(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private GradientDrawable roundGradientDrawable(int c1, int c2, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private int bgColor() {
        if (theme == 1) return Color.rgb(16, 4, 8);
        if (theme == 2) return Color.rgb(3, 8, 20);
        if (theme == 3) return Color.rgb(2, 14, 9);
        if (theme == 4) return Color.BLACK;
        if (theme == 5) return Color.rgb(4, 13, 18);
        return Color.rgb(8, 7, 18);
    }

    private int cardColor() {
        if (theme == 1) return Color.rgb(42, 10, 18);
        if (theme == 2) return Color.rgb(9, 24, 48);
        if (theme == 3) return Color.rgb(8, 34, 22);
        if (theme == 4) return Color.rgb(10, 10, 12);
        if (theme == 5) return Color.rgb(8, 35, 42);
        return Color.rgb(27, 20, 40);
    }

    private int cardAltColor() {
        if (theme == 1) return Color.rgb(18, 8, 14);
        if (theme == 2) return Color.rgb(5, 10, 24);
        if (theme == 3) return Color.rgb(4, 18, 12);
        if (theme == 4) return Color.rgb(2, 2, 2);
        if (theme == 5) return Color.rgb(6, 15, 24);
        return Color.rgb(18, 10, 31);
    }

    private int searchColor() {
        return Color.argb(58, 255, 255, 255);
    }

    private int dockColor() {
        return Color.argb(94, 10, 10, 18);
    }

    private int textColor() {
        return Color.rgb(244, 238, 255);
    }

    private int mutedColor() {
        return Color.rgb(174, 164, 198);
    }

    private int accentColor() {
        if (theme == 1) return Color.rgb(255, 69, 78);
        if (theme == 2) return Color.rgb(52, 216, 255);
        if (theme == 3) return Color.rgb(124, 255, 107);
        if (theme == 4) return Color.rgb(255, 255, 255);
        if (theme == 5) return Color.rgb(170, 238, 255);
        return Color.rgb(157, 77, 255);
    }

    private int accent2Color() {
        if (theme == 1) return Color.rgb(255, 156, 70);
        if (theme == 2) return Color.rgb(121, 100, 255);
        if (theme == 3) return Color.rgb(52, 216, 255);
        if (theme == 4) return Color.rgb(160, 160, 160);
        if (theme == 5) return Color.rgb(77, 255, 224);
        return Color.rgb(53, 216, 255);
    }

    private int accentSoft() {
        int c = accentColor();
        return Color.argb(120, Color.red(c), Color.green(c), Color.blue(c));
    }

    private String themeName() {
        if (theme == 1) return "ROG Red";
        if (theme == 2) return "Cyber Blue";
        if (theme == 3) return "Toxic Green";
        if (theme == 4) return "AMOLED Pure";
        if (theme == 5) return "Ice Neon";
        return "Cyber Violet";
    }

    private String shortTurboName() {
        if ("Performance".equals(turboMode)) return "Perf";
        if ("Battery".equals(turboMode)) return "Eco";
        return turboMode;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class AppsAdapter extends BaseAdapter {
        @Override public int getCount() { return visibleApps.size(); }
        @Override public Object getItem(int position) { return visibleApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout item;
            ImageView iconView;
            TextView labelView;
            TextView metaView;

            if (convertView == null) {
                item = new LinearLayout(MainActivity.this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(dp(4), dp(9), dp(4), dp(9));
                item.setBackground(roundDrawable(Color.argb(18, 255, 255, 255), dp(18), Color.argb(18, 255, 255, 255), 1));

                iconView = new ImageView(MainActivity.this);
                iconView.setId(1001);
                iconView.setAdjustViewBounds(true);
                item.addView(iconView, new LinearLayout.LayoutParams(dp(globalIconDp), dp(globalIconDp)));

                labelView = new TextView(MainActivity.this);
                labelView.setId(1002);
                labelView.setTextColor(textColor());
                labelView.setTextSize(11);
                labelView.setGravity(Gravity.CENTER);
                labelView.setMaxLines(2);
                labelView.setPadding(0, dp(6), 0, 0);
                item.addView(labelView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                metaView = new TextView(MainActivity.this);
                metaView.setId(1003);
                metaView.setTextColor(mutedColor());
                metaView.setTextSize(9);
                metaView.setGravity(Gravity.CENTER);
                item.addView(metaView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                item = (LinearLayout) convertView;
            }

            AppInfo app = visibleApps.get(position);
            iconView = item.findViewById(1001);
            labelView = item.findViewById(1002);
            metaView = item.findViewById(1003);

            int size = globalIconDp + (app.sizeMode * 10);
            if (size < 38) size = 38;
            ViewGroup.LayoutParams iconLp = iconView.getLayoutParams();
            iconLp.width = dp(size);
            iconLp.height = dp(size);
            iconView.setLayoutParams(iconLp);

            iconView.setImageDrawable(app.icon);
            labelView.setText(app.displayName());

            String meta = "";
            if (app.favorite) meta += "★";
            if (app.hidden) meta += " oculto";
            if (MODE_USED.equals(currentMode)) meta += (meta.length() > 0 ? " • " : "") + app.openCount + "x";
            if (MODE_RECENT.equals(currentMode) && app.lastOpened > 0) meta += (meta.length() > 0 ? " • " : "") + "recente";
            if (!CAT_ALL.equals(app.category) && MODE_ALL.equals(currentMode)) meta += (meta.length() > 0 ? " • " : "") + categoryLabel(app.category);
            metaView.setText(meta);

            item.setAlpha(app.hidden ? 0.45f : 1f);
            return item;
        }
    }

    private String categoryLabel(String category) {
        if (CAT_GAMES.equals(category)) return "game";
        if (CAT_SOCIAL.equals(category)) return "social";
        if (CAT_MEDIA.equals(category)) return "mídia";
        if (CAT_TOOLS.equals(category)) return "tool";
        if (CAT_SYSTEM.equals(category)) return "sys";
        return "app";
    }

    private static class AppInfo {
        final String originalLabel;
        final String packageName;
        final Drawable icon;
        String alias = "";
        boolean favorite = false;
        boolean hidden = false;
        long lastOpened = 0L;
        int openCount = 0;
        int sizeMode = 0;
        String category = CAT_ALL;

        AppInfo(String originalLabel, String packageName, Drawable icon) {
            this.originalLabel = originalLabel;
            this.packageName = packageName;
            this.icon = icon;
        }

        String displayName() {
            if (alias != null && alias.trim().length() > 0) return alias.trim();
            return originalLabel;
        }
    }

    private class CircuitBackgroundView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);

        CircuitBackgroundView(Context context) {
            super(context);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            canvas.drawColor(bgColor());
            int a = accentColor();

            glow.setStyle(Paint.Style.FILL);
            glow.setColor(Color.argb(28, Color.red(a), Color.green(a), Color.blue(a)));
            canvas.drawCircle(w * 0.85f, h * 0.12f, dp(120), glow);
            canvas.drawCircle(w * 0.20f, h * 0.72f, dp(150), glow);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.argb(35, Color.red(a), Color.green(a), Color.blue(a)));
            for (int i = -w; i < w * 2; i += dp(54)) {
                canvas.drawLine(i, 0, i + h / 2, h, paint);
            }

            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(54, Color.red(a), Color.green(a), Color.blue(a)));
            RectF r = new RectF(dp(24), dp(130), w - dp(24), h - dp(120));
            canvas.drawRoundRect(r, dp(34), dp(34), paint);
        }
    }

    private class HorizontalCategoryRow extends HorizontalScrollView {
        final LinearLayout inner;
        HorizontalCategoryRow(Context context) {
            super(context);
            setHorizontalScrollBarEnabled(false);
            setVerticalScrollBarEnabled(false);
            inner = new LinearLayout(context);
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setGravity(Gravity.CENTER_VERTICAL);
            inner.setPadding(0, dp(5), 0, dp(5));
            addView(inner, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }
}

