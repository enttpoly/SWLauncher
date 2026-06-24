package com.sw.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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

    private static final String CAT_ALL = "all";
    private static final String CAT_GAMES = "games";
    private static final String CAT_SOCIAL = "social";
    private static final String CAT_MEDIA = "media";
    private static final String CAT_TOOLS = "tools";
    private static final String CAT_SYSTEM = "system";

    private SharedPreferences prefs;
    private FrameLayout rootFrame;
    private LinearLayout root;
    private GridView grid;
    private AppsAdapter adapter;
    private EditText searchBox;
    private TextView timeText;
    private TextView dateText;
    private TextView stateText;
    private TextView emptyView;
    private LinearLayout modeRow;
    private LinearLayout categoryRow;
    private LinearLayout alphabetRail;

    private final ArrayList<AppInfo> allApps = new ArrayList<>();
    private final ArrayList<AppInfo> visibleApps = new ArrayList<>();
    private final ArrayList<Button> modeButtons = new ArrayList<>();
    private final ArrayList<Button> categoryButtons = new ArrayList<>();

    private String currentMode = MODE_HOME;
    private String currentCategory = CAT_ALL;
    private String query = "";
    private int columns = 4;
    private int iconDp = 58;
    private int theme = 0;
    private String profile = "Expressive";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateHeader();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadSettings();
        buildUi();
        loadApps();
        handler.post(clockTick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        updateHeader();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(clockTick);
    }

    private void loadSettings() {
        columns = prefs.getInt("columns", 4);
        iconDp = prefs.getInt("global_icon_dp", 58);
        theme = prefs.getInt("m3_theme", 0);
        profile = prefs.getString("m3_profile", "Expressive");
        currentMode = prefs.getString("last_mode", MODE_HOME);
        currentCategory = prefs.getString("last_category", CAT_ALL);
    }

    private void saveSettings() {
        prefs.edit()
                .putInt("columns", columns)
                .putInt("global_icon_dp", iconDp)
                .putInt("m3_theme", theme)
                .putString("m3_profile", profile)
                .putString("last_mode", currentMode)
                .putString("last_category", currentCategory)
                .apply();
    }

    private void buildUi() {
        modeButtons.clear();
        categoryButtons.clear();

        Window window = getWindow();
        window.setStatusBarColor(bgTop());
        window.setNavigationBarColor(bgBottom());

        rootFrame = new FrameLayout(this);
        rootFrame.setBackground(backgroundGradient());
        rootFrame.addView(new ExpressiveBlobView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));
        rootFrame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        buildTopBar();
        buildHeroCard();
        buildSearchBar();
        buildModeTabs();
        buildCategoryTabs();
        buildGridArea();
        buildBottomBar();

        setContentView(rootFrame);
    }

    private void buildTopBar() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView title = text("SW Launcher", 21, textColor(), true);
        titles.addView(title);

        TextView sub = text("Material 3 Expressive • Android 17", 12, mutedColor(), false);
        sub.setLetterSpacing(0.06f);
        titles.addView(sub);

        Button styleButton = roundButton("Style", accentColor(), onAccentColor(), dp(13));
        styleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStyleDialog();
            }
        });
        row.addView(styleButton, new LinearLayout.LayoutParams(dp(84), dp(44)));

        Button configButton = iconButton("⚙");
        configButton.setTextSize(22);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        cLp.leftMargin = dp(10);
        row.addView(configButton, cLp);

        root.addView(row);
    }

    private void buildHeroCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(cardDrawable(surfaceColor(), dp(32), accentSoft(), 1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        top.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        timeText = text("--:--", 42, textColor(), true);
        timeText.setLetterSpacing(-0.03f);
        left.addView(timeText);

        dateText = text("", 24, textColor(), true);
        left.addView(dateText);

        TextView badge = text("M3E CORE", 12, onAccentColor(), true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(cardDrawable(accentColor(), dp(999), Color.TRANSPARENT, 0));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(92), dp(34));
        top.addView(badge, bLp);

        card.addView(top);

        stateText = text("", 13, mutedColor(), false);
        stateText.setPadding(0, dp(10), 0, 0);
        card.addView(stateText);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(16);
        root.addView(card, lp);
    }

    private void buildSearchBar() {
        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Pesquisar apps...");
        searchBox.setTextColor(textColor());
        searchBox.setHintTextColor(mutedColor());
        searchBox.setTextSize(16);
        searchBox.setInputType(InputType.TYPE_CLASS_TEXT);
        searchBox.setPadding(dp(18), 0, dp(18), 0);
        searchBox.setBackground(cardDrawable(surfaceElevated(), dp(28), accentSoft(), 1));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString().trim();
                filterApps();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        lp.topMargin = dp(14);
        root.addView(searchBox, lp);
    }

    private void buildModeTabs() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setPadding(0, 0, 0, 0);
        scroll.addView(modeRow);

        addMode("Início", MODE_HOME);
        addMode("Apps", MODE_ALL);
        addMode("Recentes", MODE_RECENT);
        addMode("Usados", MODE_USED);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        lp.topMargin = dp(12);
        root.addView(scroll, lp);
        updateModeButtons();
    }

    private void buildCategoryTabs() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        categoryRow = new LinearLayout(this);
        categoryRow.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(categoryRow);

        addCategory("Todos", CAT_ALL);
        addCategory("Jogos", CAT_GAMES);
        addCategory("Social", CAT_SOCIAL);
        addCategory("Mídia", CAT_MEDIA);
        addCategory("Ferramentas", CAT_TOOLS);
        addCategory("Sistema", CAT_SYSTEM);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        lp.topMargin = dp(2);
        root.addView(scroll, lp);
        updateCategoryButtons();
    }

    private void buildGridArea() {
        FrameLayout gridWrap = new FrameLayout(this);

        grid = new GridView(this);
        grid.setNumColumns(columns);
        grid.setVerticalSpacing(dp(14));
        grid.setHorizontalSpacing(dp(10));
        grid.setPadding(0, dp(8), dp(18), dp(78));
        grid.setClipToPadding(false);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        adapter = new AppsAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < visibleApps.size()) openApp(visibleApps.get(position));
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < visibleApps.size()) showAppSheet(visibleApps.get(position));
                return true;
            }
        });
        gridWrap.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        emptyView = text("", 15, mutedColor(), false);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(24), dp(28), dp(24), dp(28));
        emptyView.setBackground(cardDrawable(surfaceColor(), dp(30), Color.TRANSPARENT, 0));
        FrameLayout.LayoutParams evLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        evLp.gravity = Gravity.CENTER;
        evLp.leftMargin = dp(8);
        evLp.rightMargin = dp(28);
        gridWrap.addView(emptyView, evLp);

        alphabetRail = new LinearLayout(this);
        alphabetRail.setOrientation(LinearLayout.VERTICAL);
        alphabetRail.setGravity(Gravity.CENTER);
        alphabetRail.setPadding(0, dp(4), 0, dp(4));
        alphabetRail.setBackground(cardDrawable(railColor(), dp(999), accentSoft(), 1));
        buildAlphabetRail();
        FrameLayout.LayoutParams railLp = new FrameLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.WRAP_CONTENT);
        railLp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        gridWrap.addView(alphabetRail, railLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        );
        root.addView(gridWrap, lp);
    }

    private void buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackground(cardDrawable(surfaceElevated(), dp(34), accentSoft(), 1));

        addBottom(bar, "⌂", "Home", new View.OnClickListener() { @Override public void onClick(View v) { setMode(MODE_HOME); } });
        addBottom(bar, "⌕", "Buscar", new View.OnClickListener() { @Override public void onClick(View v) { focusSearch(); } });
        addBottom(bar, "✦", "Style", new View.OnClickListener() { @Override public void onClick(View v) { showStyleDialog(); } });
        addBottom(bar, "⚙", "Config", new View.OnClickListener() { @Override public void onClick(View v) { showSettingsDialog(); } });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(76)
        );
        lp.gravity = Gravity.BOTTOM;
        lp.leftMargin = dp(18);
        lp.rightMargin = dp(18);
        lp.bottomMargin = dp(12);
        rootFrame.addView(bar, lp);
    }

    private void addBottom(LinearLayout bar, String icon, String label, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), 0, dp(4), 0);
        item.setOnClickListener(listener);

        TextView i = text(icon, 21, accentColor(), true);
        i.setGravity(Gravity.CENTER);
        item.addView(i);

        TextView l = text(label, 10, mutedColor(), false);
        l.setGravity(Gravity.CENTER);
        item.addView(l);

        bar.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void addMode(String label, final String mode) {
        final Button b = roundButton(label, Color.TRANSPARENT, textColor(), dp(15));
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(mode); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(118), dp(46));
        lp.rightMargin = dp(8);
        modeRow.addView(b, lp);
        modeButtons.add(b);
    }

    private void addCategory(String label, final String cat) {
        final Button b = roundButton(label, Color.TRANSPARENT, mutedColor(), dp(999));
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                currentCategory = cat;
                saveSettings();
                filterApps();
                updateCategoryButtons();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.rightMargin = dp(8);
        b.setMinWidth(dp(80));
        categoryRow.addView(b, lp);
        categoryButtons.add(b);
    }

    private void setMode(String mode) {
        currentMode = mode;
        saveSettings();
        filterApps();
        updateModeButtons();
    }

    private void updateModeButtons() {
        String[] modes = new String[] { MODE_HOME, MODE_ALL, MODE_RECENT, MODE_USED };
        for (int i = 0; i < modeButtons.size(); i++) {
            Button b = modeButtons.get(i);
            boolean active = modes[i].equals(currentMode);
            b.setTextColor(active ? onAccentColor() : mutedColor());
            b.setBackground(cardDrawable(active ? accentColor() : surfaceColor(), dp(18), active ? accent2Color() : Color.TRANSPARENT, active ? 2 : 0));
        }
    }

    private void updateCategoryButtons() {
        String[] cats = new String[] { CAT_ALL, CAT_GAMES, CAT_SOCIAL, CAT_MEDIA, CAT_TOOLS, CAT_SYSTEM };
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button b = categoryButtons.get(i);
            boolean active = cats[i].equals(currentCategory);
            b.setTextColor(active ? onAccentColor() : mutedColor());
            b.setBackground(cardDrawable(active ? accent2Color() : surfaceSoft(), dp(999), active ? accentColor() : Color.TRANSPARENT, active ? 2 : 0));
        }
    }

    private void buildAlphabetRail() {
        alphabetRail.removeAllViews();
        for (char c = 'A'; c <= 'Z'; c++) {
            final String letter = String.valueOf(c);
            TextView t = text(letter, 9, mutedColor(), true);
            t.setGravity(Gravity.CENTER);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { scrollToLetter(letter); }
            });
            alphabetRail.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        }
    }

    private void loadApps() {
        ArrayList<AppInfo> loaded = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : infos) {
            String pkg = info.activityInfo.packageName;
            String name = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            loaded.add(new AppInfo(name, customName(pkg, name), pkg, icon, detectCategory(pkg, name)));
        }
        Collections.sort(loaded, new Comparator<AppInfo>() {
            @Override public int compare(AppInfo a, AppInfo b) {
                return a.displayName.toLowerCase(Locale.getDefault()).compareTo(b.displayName.toLowerCase(Locale.getDefault()));
            }
        });
        allApps.clear();
        allApps.addAll(loaded);
        filterApps();
    }

    private void filterApps() {
        visibleApps.clear();
        Set<String> pins = getStringSet("pins");
        Set<String> hidden = getStringSet("hidden");
        ArrayList<String> recents = getList("recents");

        for (AppInfo app : allApps) {
            if (hidden.contains(app.packageName) && !MODE_ALL.equals(currentMode)) continue;
            if (!query.isEmpty() && !app.displayName.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) continue;
            if (!CAT_ALL.equals(currentCategory) && !currentCategory.equals(app.category)) continue;

            if (MODE_HOME.equals(currentMode) && !pins.contains(app.packageName)) continue;
            if (MODE_RECENT.equals(currentMode) && !recents.contains(app.packageName)) continue;
            if (MODE_USED.equals(currentMode) && usageCount(app.packageName) <= 0) continue;

            visibleApps.add(app);
        }

        if (MODE_RECENT.equals(currentMode)) {
            Collections.sort(visibleApps, new Comparator<AppInfo>() {
                @Override public int compare(AppInfo a, AppInfo b) {
                    return Integer.valueOf(recents.indexOf(a.packageName)).compareTo(recents.indexOf(b.packageName));
                }
            });
        }
        if (MODE_USED.equals(currentMode)) {
            Collections.sort(visibleApps, new Comparator<AppInfo>() {
                @Override public int compare(AppInfo a, AppInfo b) {
                    return Integer.valueOf(usageCount(b.packageName)).compareTo(usageCount(a.packageName));
                }
            });
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyView == null) return;
        if (visibleApps.isEmpty()) {
            String msg;
            if (MODE_HOME.equals(currentMode)) msg = "Sua Home está limpa.\nSegure um app na aba Apps e toque em Fixar.";
            else if (MODE_RECENT.equals(currentMode)) msg = "Sem recentes ainda.\nAbra apps pelo SW Launcher para preencher esta área.";
            else if (MODE_USED.equals(currentMode)) msg = "Sem uso registrado.\nOs apps mais abertos aparecerão aqui.";
            else msg = "Nenhum app encontrado.";
            emptyView.setText(msg);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void scrollToLetter(String letter) {
        for (int i = 0; i < visibleApps.size(); i++) {
            if (visibleApps.get(i).displayName.toUpperCase(Locale.getDefault()).startsWith(letter)) {
                grid.smoothScrollToPosition(i);
                return;
            }
        }
        Toast.makeText(this, "Sem apps em " + letter, Toast.LENGTH_SHORT).show();
    }

    private void openApp(AppInfo app) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launch != null) {
            addRecent(app.packageName);
            prefs.edit().putInt("usage_" + app.packageName, usageCount(app.packageName) + 1).apply();
            startActivity(launch);
        } else {
            Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAppSheet(final AppInfo app) {
        final Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        header.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(14), 0, 0, 0);
        names.addView(text(app.displayName, 22, textColor(), true));
        names.addView(text(app.packageName, 11, mutedColor(), false));
        header.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(header);

        addDivider(box);

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        addSheetAction(quick, "Abrir", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); openApp(app); } });
        addSheetAction(quick, isPinned(app.packageName) ? "Desfixar" : "Fixar", new View.OnClickListener() { @Override public void onClick(View v) { togglePin(app.packageName); dialog.dismiss(); } });
        addSheetAction(quick, "Info", new View.OnClickListener() { @Override public void onClick(View v) { openAppInfo(app.packageName); dialog.dismiss(); } });
        box.addView(quick);

        addListAction(box, "Editar nome exibido", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); editName(app); } });
        addListAction(box, "Tamanho deste app", new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); chooseIconSize(app); } });
        addListAction(box, isHidden(app.packageName) ? "Mostrar app" : "Ocultar app", new View.OnClickListener() { @Override public void onClick(View v) { toggleHidden(app.packageName); dialog.dismiss(); } });
        addListAction(box, "Cache e dados", new View.OnClickListener() { @Override public void onClick(View v) { openAppInfo(app.packageName); dialog.dismiss(); } });
        addListAction(box, "Desinstalar", new View.OnClickListener() { @Override public void onClick(View v) { uninstallApp(app.packageName); dialog.dismiss(); } });

        dialog.setContentView(box);
        dialog.show();
    }

    private void showSettingsDialog() {
        final Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Configurações", 25, textColor(), true));
        box.addView(text("Material 3 Expressive • Android 17", 13, mutedColor(), false));
        addDivider(box);

        addListAction(box, "Colunas da grade: " + columns, new View.OnClickListener() {
            @Override public void onClick(View v) {
                columns = columns >= 6 ? 3 : columns + 1;
                grid.setNumColumns(columns);
                saveSettings();
                Toast.makeText(MainActivity.this, "Colunas: " + columns, Toast.LENGTH_SHORT).show();
            }
        });
        addListAction(box, "Tamanho global dos ícones: " + iconDp + "dp", new View.OnClickListener() {
            @Override public void onClick(View v) {
                iconDp += 6;
                if (iconDp > 82) iconDp = 46;
                saveSettings();
                adapter.notifyDataSetChanged();
            }
        });
        addListAction(box, "Tema visual", new View.OnClickListener() { @Override public void onClick(View v) { showStyleDialog(); } });
        addListAction(box, "Permissão de uso real", new View.OnClickListener() { @Override public void onClick(View v) { openUsageAccess(); } });
        addListAction(box, "Apps instalados", new View.OnClickListener() { @Override public void onClick(View v) { openManageApps(); } });
        addListAction(box, "Resetar layout", new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.edit().clear().apply();
                Toast.makeText(MainActivity.this, "Layout resetado", Toast.LENGTH_SHORT).show();
                recreate();
            }
        });
        addListAction(box, "Sobre a v0.4", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Toast.makeText(MainActivity.this, "SW Launcher v0.4 M3 Expressive", Toast.LENGTH_LONG).show();
            }
        });

        dialog.setContentView(box);
        dialog.show();
    }

    private void showStyleDialog() {
        final Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Style", 25, textColor(), true));
        box.addView(text("Escolha uma paleta Material 3 Expressive", 13, mutedColor(), false));
        addDivider(box);

        final String[] names = new String[]{"Pixel Violet", "Android Blue", "Mint Pulse", "Rose Pop", "AMOLED Ink"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            addListAction(box, (theme == idx ? "✓ " : "") + names[i], new View.OnClickListener() {
                @Override public void onClick(View v) {
                    theme = idx;
                    saveSettings();
                    dialog.dismiss();
                    recreate();
                }
            });
        }

        dialog.setContentView(box);
        dialog.show();
    }

    private void editName(final AppInfo app) {
        final Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Editar nome", 24, textColor(), true));
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(app.displayName);
        input.setTextColor(textColor());
        input.setHintTextColor(mutedColor());
        input.setTextSize(18);
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setBackground(cardDrawable(surfaceElevated(), dp(22), accentSoft(), 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        lp.topMargin = dp(14);
        box.addView(input, lp);
        Button save = roundButton("Salvar", accentColor(), onAccentColor(), dp(22));
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String value = input.getText().toString().trim();
                prefs.edit().putString("name_" + app.packageName, value.isEmpty() ? app.name : value).apply();
                dialog.dismiss();
                loadApps();
            }
        });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        sLp.topMargin = dp(14);
        box.addView(save, sLp);
        dialog.setContentView(box);
        dialog.show();
        input.requestFocus();
        input.postDelayed(new Runnable() { @Override public void run() { showKeyboard(input); } }, 250);
    }

    private void chooseIconSize(final AppInfo app) {
        final Dialog dialog = baseDialog();
        LinearLayout box = dialogBox();
        box.addView(text("Tamanho do app", 24, textColor(), true));
        int[] sizes = new int[]{46, 52, 58, 66, 74, 82};
        for (final int size : sizes) {
            addListAction(box, size + "dp", new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.edit().putInt("icon_" + app.packageName, size).apply();
                    dialog.dismiss();
                    adapter.notifyDataSetChanged();
                }
            });
        }
        dialog.setContentView(box);
        dialog.show();
    }

    private Dialog baseDialog() {
        Dialog dialog = new Dialog(this);
        Window w = dialog.getWindow();
        return dialog;
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(22), dp(22), dp(22));
        box.setBackground(cardDrawable(dialogColor(), dp(34), accentSoft(), 1));
        ScrollView scroll = null;
        return box;
    }

    private void addSheetAction(LinearLayout row, String label, View.OnClickListener l) {
        Button b = roundButton(label, surfaceElevated(), textColor(), dp(22));
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(56), 1);
        lp.rightMargin = dp(8);
        row.addView(b, lp);
    }

    private void addListAction(LinearLayout box, String label, View.OnClickListener l) {
        TextView item = text(label, 16, textColor(), false);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(16), 0, dp(16), 0);
        item.setBackground(cardDrawable(surfaceSoft(), dp(20), Color.TRANSPARENT, 0));
        item.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.topMargin = dp(9);
        box.addView(item, lp);
    }

    private void addDivider(LinearLayout box) {
        View v = new View(this);
        v.setBackgroundColor(accentSoft());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.topMargin = dp(16);
        lp.bottomMargin = dp(8);
        box.addView(v, lp);
    }

    private void togglePin(String pkg) {
        Set<String> set = getStringSet("pins");
        if (set.contains(pkg)) set.remove(pkg); else set.add(pkg);
        putStringSet("pins", set);
        filterApps();
    }

    private void toggleHidden(String pkg) {
        Set<String> set = getStringSet("hidden");
        if (set.contains(pkg)) set.remove(pkg); else set.add(pkg);
        putStringSet("hidden", set);
        filterApps();
    }

    private boolean isPinned(String pkg) { return getStringSet("pins").contains(pkg); }
    private boolean isHidden(String pkg) { return getStringSet("hidden").contains(pkg); }
    private int usageCount(String pkg) { return prefs.getInt("usage_" + pkg, 0); }

    private String customName(String pkg, String fallback) {
        return prefs.getString("name_" + pkg, fallback);
    }

    private void addRecent(String pkg) {
        ArrayList<String> list = getList("recents");
        list.remove(pkg);
        list.add(0, pkg);
        while (list.size() > 24) list.remove(list.size() - 1);
        prefs.edit().putString("recents", join(list)).apply();
    }

    private ArrayList<String> getList(String key) {
        String raw = prefs.getString(key, "");
        ArrayList<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return list;
        String[] parts = raw.split(",");
        for (String p : parts) if (!p.trim().isEmpty()) list.add(p.trim());
        return list;
    }

    private String join(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        }
        return sb.toString();
    }

    private Set<String> getStringSet(String key) {
        return new HashSet<>(prefs.getStringSet(key, new HashSet<String>()));
    }

    private void putStringSet(String key, Set<String> set) {
        prefs.edit().putStringSet(key, new HashSet<>(set)).apply();
    }

    private String detectCategory(String pkg, String name) {
        String s = (pkg + " " + name).toLowerCase(Locale.getDefault());
        if (s.contains("roblox") || s.contains("brawl") || s.contains("game") || s.contains("gaming") || s.contains("kick") || s.contains("discord")) return CAT_GAMES;
        if (s.contains("instagram") || s.contains("whatsapp") || s.contains("facebook") || s.contains("telegram") || s.contains("discord") || s.contains("gmail")) return CAT_SOCIAL;
        if (s.contains("youtube") || s.contains("spotify") || s.contains("music") || s.contains("camera") || s.contains("galeria") || s.contains("photo") || s.contains("capcut") || s.contains("bandlab")) return CAT_MEDIA;
        if (s.contains("settings") || s.contains("config") || s.contains("calcul") || s.contains("files") || s.contains("arquivos") || s.contains("termux")) return CAT_TOOLS;
        if (pkg.startsWith("com.android") || pkg.startsWith("com.samsung") || pkg.startsWith("com.google.android")) return CAT_SYSTEM;
        return CAT_ALL;
    }

    private void openAppInfo(String pkg) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir informações", Toast.LENGTH_SHORT).show();
        }
    }

    private void uninstallApp(String pkg) {
        try {
            Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível desinstalar", Toast.LENGTH_SHORT).show();
        }
    }

    private void openUsageAccess() {
        try { startActivity(new Intent("android.settings.USAGE_ACCESS_SETTINGS")); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void openManageApps() {
        try { startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void focusSearch() {
        setMode(MODE_ALL);
        searchBox.requestFocus();
        showKeyboard(searchBox);
    }

    private void showKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    private void updateHeader() {
        if (timeText == null) return;
        Date now = new Date();
        timeText.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        dateText.setText(new SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now).replace(".", ""));
        int battery = batteryPercent();
        stateText.setText(profile + " profile • Bateria " + battery + "% • " + allApps.size() + " apps • API 37 ready");
    }

    private int batteryPercent() {
        try {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            if (bm != null) return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception ignored) { }
        return 0;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button roundButton(String label, int bg, int fg, int radius) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(fg);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(cardDrawable(bg, radius, Color.TRANSPARENT, 0));
        return b;
    }

    private Button iconButton(String label) {
        Button b = roundButton(label, surfaceElevated(), textColor(), dp(999));
        b.setGravity(Gravity.CENTER);
        return b;
    }

    private GradientDrawable backgroundGradient() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ bgTop(), bgMid(), bgBottom() });
    }

    private GradientDrawable cardDrawable(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) d.setStroke(dp(strokeWidth), strokeColor);
        return d;
    }

    private int bgTop() {
        switch (theme) {
            case 1: return Color.rgb(5, 16, 36);
            case 2: return Color.rgb(5, 28, 22);
            case 3: return Color.rgb(35, 10, 26);
            case 4: return Color.rgb(0, 0, 0);
            default: return Color.rgb(19, 12, 35);
        }
    }

    private int bgMid() {
        switch (theme) {
            case 1: return Color.rgb(8, 30, 60);
            case 2: return Color.rgb(10, 46, 35);
            case 3: return Color.rgb(54, 18, 44);
            case 4: return Color.rgb(7, 7, 9);
            default: return Color.rgb(34, 18, 57);
        }
    }

    private int bgBottom() {
        switch (theme) {
            case 1: return Color.rgb(6, 12, 24);
            case 2: return Color.rgb(5, 18, 14);
            case 3: return Color.rgb(22, 8, 18);
            case 4: return Color.rgb(0, 0, 0);
            default: return Color.rgb(10, 7, 18);
        }
    }

    private int accentColor() {
        switch (theme) {
            case 1: return Color.rgb(91, 162, 255);
            case 2: return Color.rgb(77, 222, 174);
            case 3: return Color.rgb(255, 108, 169);
            case 4: return Color.rgb(255, 255, 255);
            default: return Color.rgb(202, 163, 255);
        }
    }

    private int accent2Color() {
        switch (theme) {
            case 1: return Color.rgb(118, 230, 255);
            case 2: return Color.rgb(185, 255, 193);
            case 3: return Color.rgb(255, 196, 220);
            case 4: return Color.rgb(140, 140, 150);
            default: return Color.rgb(147, 220, 255);
        }
    }

    private int onAccentColor() {
        return theme == 4 ? Color.rgb(8, 8, 10) : Color.rgb(27, 16, 35);
    }

    private int textColor() { return Color.rgb(250, 247, 255); }
    private int mutedColor() { return Color.rgb(204, 194, 220); }
    private int surfaceColor() { return Color.argb(190, 36, 30, 48); }
    private int surfaceSoft() { return Color.argb(126, 255, 255, 255); }
    private int surfaceElevated() { return Color.argb(220, 48, 39, 63); }
    private int dialogColor() { return Color.rgb(42, 36, 52); }
    private int railColor() { return Color.argb(96, 255, 255, 255); }
    private int accentSoft() { return Color.argb(105, Color.red(accentColor()), Color.green(accentColor()), Color.blue(accentColor())); }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class AppsAdapter extends BaseAdapter {
        @Override public int getCount() { return visibleApps.size(); }
        @Override public Object getItem(int position) { return visibleApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppInfo app = visibleApps.get(position);
            LinearLayout item = new LinearLayout(MainActivity.this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(8), dp(4), dp(8));

            int size = prefs.getInt("icon_" + app.packageName, iconDp);
            FrameLayout iconBubble = new FrameLayout(MainActivity.this);
            iconBubble.setBackground(cardDrawable(Color.argb(190, 255, 255, 255), dp(24), accentSoft(), 1));
            ImageView iv = new ImageView(MainActivity.this);
            iv.setImageDrawable(app.icon);
            iv.setPadding(dp(7), dp(7), dp(7), dp(7));
            iconBubble.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(dp(size), dp(size));
            item.addView(iconBubble, iLp);

            TextView label = text(app.displayName, 12, textColor(), false);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lLp.topMargin = dp(6);
            item.addView(label, lLp);
            return item;
        }
    }

    private class AppInfo {
        String name;
        String displayName;
        String packageName;
        Drawable icon;
        String category;
        AppInfo(String name, String displayName, String packageName, Drawable icon, String category) {
            this.name = name;
            this.displayName = displayName;
            this.packageName = packageName;
            this.icon = icon;
            this.category = category;
        }
    }

    private class ExpressiveBlobView extends View {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rect = new RectF();
        public ExpressiveBlobView(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(36, Color.red(accentColor()), Color.green(accentColor()), Color.blue(accentColor())));
            rect.set(-dp(80), dp(80), w * 0.72f, dp(340));
            canvas.drawRoundRect(rect, dp(120), dp(120), paint);
            paint.setColor(Color.argb(28, Color.red(accent2Color()), Color.green(accent2Color()), Color.blue(accent2Color())));
            rect.set(w * 0.30f, h * 0.48f, w + dp(120), h * 0.82f);
            canvas.drawRoundRect(rect, dp(160), dp(160), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.argb(38, 255, 255, 255));
            for (int i = 0; i < 6; i++) {
                float y = h * 0.18f + i * dp(88);
                canvas.drawLine(dp(18), y, w - dp(18), y, paint);
            }
        }
    }
}
