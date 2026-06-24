package com.sw.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
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

    private final ArrayList<AppInfo> allApps = new ArrayList<>();
    private final ArrayList<AppInfo> visibleApps = new ArrayList<>();

    private SharedPreferences prefs;
    private AppsAdapter adapter;
    private GridView grid;
    private LinearLayout root;
    private LinearLayout alphabetRail;
    private TextView clockText;
    private TextView subtitleText;
    private EditText searchBox;

    private String currentMode = MODE_HOME;
    private String currentSearch = "";
    private int columns = 4;
    private int globalIconDp = 56;
    private int theme = 0;
    private boolean showHiddenInAll = false;

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
    }

    private void saveLauncherSettings() {
        prefs.edit()
                .putInt("columns", columns)
                .putInt("global_icon_dp", globalIconDp)
                .putInt("theme", theme)
                .putBoolean("show_hidden_in_all", showHiddenInAll)
                .putString("last_mode", currentMode)
                .apply();
    }

    private void buildHomeUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(12), dp(10));
        applyThemeBackground();

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("SW Launcher");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button settingsButton = pillButton("⚙", 42);
        settingsButton.setTextSize(20);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLauncherSettings();
            }
        });
        topRow.addView(settingsButton, new LinearLayout.LayoutParams(dp(48), dp(42)));
        root.addView(topRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        clockText = new TextView(this);
        clockText.setTextColor(Color.WHITE);
        clockText.setTextSize(34);
        clockText.setTypeface(Typeface.DEFAULT_BOLD);
        clockText.setGravity(Gravity.START);
        clockText.setPadding(0, dp(12), 0, dp(6));
        root.addView(clockText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        subtitleText = new TextView(this);
        subtitleText.setTextColor(Color.argb(200, 255, 255, 255));
        subtitleText.setTextSize(12);
        subtitleText.setPadding(0, 0, 0, dp(8));
        root.addView(subtitleText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Pesquisar apps...");
        searchBox.setHintTextColor(Color.argb(150, 255, 255, 255));
        searchBox.setTextColor(Color.WHITE);
        searchBox.setTextSize(14);
        searchBox.setPadding(dp(14), 0, dp(14), 0);
        searchBox.setInputType(InputType.TYPE_CLASS_TEXT);
        searchBox.setBackground(roundDrawable(Color.argb(42, 255, 255, 255), dp(18), Color.argb(40, 255, 255, 255), 1));
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        root.addView(searchBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER);
        modeRow.setPadding(0, dp(10), 0, dp(8));
        addModeButton(modeRow, "Início", MODE_HOME);
        addModeButton(modeRow, "Apps", MODE_ALL);
        addModeButton(modeRow, "Recentes", MODE_RECENT);
        addModeButton(modeRow, "Usados", MODE_USED);
        root.addView(modeRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout contentRow = new LinearLayout(this);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);

        grid = new GridView(this);
        grid.setNumColumns(columns);
        grid.setVerticalSpacing(dp(14));
        grid.setHorizontalSpacing(dp(8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setGravity(Gravity.CENTER);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(4), 0, dp(80));
        grid.setSelector(android.R.color.transparent);

        adapter = new AppsAdapter();
        grid.setAdapter(adapter);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openApp(visibleApps.get(position));
            }
        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                openAppMenu(visibleApps.get(position));
                return true;
            }
        });

        contentRow.addView(grid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        ScrollView alphaScroll = new ScrollView(this);
        alphaScroll.setFillViewport(false);
        alphabetRail = new LinearLayout(this);
        alphabetRail.setOrientation(LinearLayout.VERTICAL);
        alphabetRail.setGravity(Gravity.CENTER_HORIZONTAL);
        alphaScroll.addView(alphabetRail, new ScrollView.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT));
        contentRow.addView(alphaScroll, new LinearLayout.LayoutParams(dp(32), ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(contentRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(8), dp(8), dp(8));
        dock.setBackground(roundDrawable(Color.argb(52, 255, 255, 255), dp(24), Color.argb(32, 255, 255, 255), 1));
        addDockButton(dock, "⚙ Config", new View.OnClickListener() {
            @Override public void onClick(View v) { openLauncherSettings(); }
        });
        addDockButton(dock, "★ Fixos", new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(MODE_HOME); }
        });
        addDockButton(dock, "🕘 Recentes", new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(MODE_RECENT); }
        });
        addDockButton(dock, "🧹 Cache", new View.OnClickListener() {
            @Override public void onClick(View v) { openCacheOptions(); }
        });
        root.addView(dock, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        setContentView(root);
        buildAlphabetRail();
        updateClock();
        updateSubtitle();
    }

    private void addModeButton(LinearLayout row, String text, final String mode) {
        Button b = pillButton(text, 38);
        b.setTextSize(12);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(mode); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(b, lp);
    }

    private void addDockButton(LinearLayout row, String text, View.OnClickListener listener) {
        Button b = pillButton(text, 42);
        b.setTextSize(11);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(b, lp);
    }

    private Button pillButton(String text, int height) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setBackground(roundDrawable(Color.argb(48, 255, 255, 255), dp(height / 2), Color.argb(35, 255, 255, 255), 1));
        return b;
    }

    private void applyThemeBackground() {
        int[] colors;
        if (theme == 1) {
            colors = new int[]{Color.rgb(3, 20, 16), Color.rgb(7, 10, 12)};
        } else if (theme == 2) {
            colors = new int[]{Color.rgb(22, 10, 34), Color.rgb(6, 7, 13)};
        } else {
            colors = new int[]{Color.rgb(12, 14, 22), Color.rgb(8, 9, 13)};
        }
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        root.setBackground(background);
    }

    private void updateClock() {
        if (clockText == null) return;
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(new Date());
        clockText.setText(time + "\n" + date);
    }

    private void updateSubtitle() {
        if (subtitleText == null) return;
        String modeName;
        if (MODE_HOME.equals(currentMode)) modeName = "Tela inicial: apps fixados podem ser movidos";
        else if (MODE_RECENT.equals(currentMode)) modeName = "Recentes: apps abertos pelo SW Launcher";
        else if (MODE_USED.equals(currentMode)) modeName = "Usados: ranking criado pelo SW Launcher";
        else if (MODE_HIDDEN.equals(currentMode)) modeName = "Apps ocultos";
        else modeName = "Gaveta de apps com rolagem por letra";
        subtitleText.setText(modeName + " • toque para abrir • segure para editar");
    }

    private void setMode(String mode) {
        currentMode = mode;
        saveLauncherSettings();
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
            readAppPrefs(app);
            allApps.add(app);
        }
        applyFilters();
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
        ArrayList<String> items = new ArrayList<>();
        items.add("Abrir");
        items.add("Editar nome");
        items.add("Tamanho do ícone");
        items.add(app.favorite ? "Remover da tela inicial" : "Fixar na tela inicial");
        if (app.favorite) {
            items.add("Mover para esquerda/cima");
            items.add("Mover para direita/baixo");
        }
        items.add(app.hidden ? "Mostrar app" : "Ocultar app");
        items.add("Info do app / limpar cache ou dados");
        items.add("Resetar edição deste app");

        final String[] arr = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(app.displayName())
                .setItems(arr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleAppMenuChoice(app, arr[which]);
                    }
                })
                .show();
    }

    private void handleAppMenuChoice(AppInfo app, String choice) {
        if (choice.equals("Abrir")) {
            openApp(app);
        } else if (choice.equals("Editar nome")) {
            renameApp(app);
        } else if (choice.equals("Tamanho do ícone")) {
            chooseAppSize(app);
        } else if (choice.contains("Fixar") || choice.contains("Remover da tela")) {
            toggleFavorite(app);
        } else if (choice.contains("esquerda")) {
            moveFavorite(app, -1);
        } else if (choice.contains("direita")) {
            moveFavorite(app, 1);
        } else if (choice.contains("Ocultar") || choice.contains("Mostrar")) {
            toggleHidden(app);
        } else if (choice.contains("Info")) {
            openAppDetails(app);
        } else if (choice.contains("Resetar")) {
            resetAppCustomization(app);
        }
    }

    private void renameApp(final AppInfo app) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(app.displayName());
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

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
        SharedPreferences.Editor ed = prefs.edit().putBoolean("fav_" + app.packageName, newValue);
        ed.apply();
        ArrayList<String> order = getHomeOrder();
        if (newValue && !order.contains(app.packageName)) order.add(app.packageName);
        if (!newValue) order.remove(app.packageName);
        saveHomeOrder(order);
        Toast.makeText(this, newValue ? "Fixado na tela inicial" : "Removido da tela inicial", Toast.LENGTH_SHORT).show();
        loadInstalledApps();
    }

    private void moveFavorite(AppInfo app, int direction) {
        ArrayList<String> order = getHomeOrder();
        if (!order.contains(app.packageName)) order.add(app.packageName);
        int index = order.indexOf(app.packageName);
        int newIndex = index + direction;
        if (newIndex < 0 || newIndex >= order.size()) return;
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
        startActivity(intent);
    }

    private void openCacheOptions() {
        String[] items = {
                "Abrir armazenamento do Android",
                "Solicitar limpeza geral de cache",
                "Abrir apps instalados"
        };
        new AlertDialog.Builder(this)
                .setTitle("Cache e dados")
                .setMessage("O Android não deixa um launcher comum apagar dados de outros apps sozinho. Essa área abre as telas oficiais para você limpar manualmente.")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) safeStart(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
                        else if (which == 1) requestClearAllCache();
                        else safeStart(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
                    }
                })
                .show();
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
        String[] items = {
                "Colunas da grade: " + columns,
                "Tamanho global dos ícones: " + globalIconDp + "dp",
                "Tema visual",
                showHiddenInAll ? "Ocultar apps escondidos na gaveta" : "Mostrar apps escondidos na gaveta",
                "Ver apps ocultos",
                "Abrir permissão de uso real",
                "Cache e dados",
                "Recarregar apps",
                "Resetar configurações do launcher",
                "Sobre esta versão"
        };
        new AlertDialog.Builder(this)
                .setTitle("Configurações do SW Launcher")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        handleSettingsChoice(which);
                    }
                })
                .show();
    }

    private void handleSettingsChoice(int which) {
        if (which == 0) chooseColumns();
        else if (which == 1) chooseGlobalIconSize();
        else if (which == 2) chooseTheme();
        else if (which == 3) {
            showHiddenInAll = !showHiddenInAll;
            saveLauncherSettings();
            loadInstalledApps();
        } else if (which == 4) {
            currentMode = MODE_HIDDEN;
            saveLauncherSettings();
            updateSubtitle();
            applyFilters();
        } else if (which == 5) {
            safeStart(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else if (which == 6) {
            openCacheOptions();
        } else if (which == 7) {
            loadInstalledApps();
            Toast.makeText(this, "Apps recarregados", Toast.LENGTH_SHORT).show();
        } else if (which == 8) {
            confirmResetLauncher();
        } else if (which == 9) {
            showAbout();
        }
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
        final String[] labels = {"Dark Grid", "Neon Green", "Purple Underground"};
        new AlertDialog.Builder(this)
                .setTitle("Tema visual")
                .setSingleChoiceItems(labels, theme, new DialogInterface.OnClickListener() {
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
                .setTitle("SW Launcher v0.2")
                .setMessage("Novidades:\n\n• Tela inicial com apps fixados\n• Mover apps fixados\n• Editar nome\n• Ajustar tamanho por app\n• Ocultar apps\n• Filtros: todos, recentes e usados\n• Rolagem por letra A-Z\n• Configurações do launcher\n• Atalhos para cache/dados nas telas oficiais do Android")
                .setPositiveButton("Fechar", null)
                .show();
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
            tv.setTextColor(Color.argb(210, 255, 255, 255));
            tv.setTextSize(10);
            tv.setGravity(Gravity.CENTER);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { scrollToLetter(letter); }
            });
            alphabetRail.addView(tv, new LinearLayout.LayoutParams(dp(26), dp(20)));
        }
    }

    private void scrollToLetter(String letter) {
        if (MODE_HOME.equals(currentMode)) {
            currentMode = MODE_ALL;
            updateSubtitle();
            saveLauncherSettings();
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

    private GradientDrawable roundDrawable(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
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
                item.setPadding(dp(3), dp(8), dp(3), dp(8));

                iconView = new ImageView(MainActivity.this);
                iconView.setId(1001);
                iconView.setAdjustViewBounds(true);
                item.addView(iconView, new LinearLayout.LayoutParams(dp(globalIconDp), dp(globalIconDp)));

                labelView = new TextView(MainActivity.this);
                labelView.setId(1002);
                labelView.setTextColor(Color.WHITE);
                labelView.setTextSize(11);
                labelView.setGravity(Gravity.CENTER);
                labelView.setMaxLines(2);
                labelView.setPadding(0, dp(6), 0, 0);
                item.addView(labelView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                metaView = new TextView(MainActivity.this);
                metaView.setId(1003);
                metaView.setTextColor(Color.argb(150, 255, 255, 255));
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
            metaView.setText(meta);

            item.setAlpha(app.hidden ? 0.45f : 1f);
            return item;
        }
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
}
