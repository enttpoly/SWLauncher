package com.sw.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private final ArrayList<AppInfo> apps = new ArrayList<>();
    private AppsAdapter adapter;
    private TextView clockText;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (clockText != null) {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                String date = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(new Date());
                clockText.setText(time + "\n" + date);
            }
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private void buildHomeUi() {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(12, 14, 22), Color.rgb(8, 9, 13)}
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(28), dp(18), dp(12));
        root.setBackground(background);

        TextView title = new TextView(this);
        title.setText("SW Launcher");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.START);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        clockText = new TextView(this);
        clockText.setTextColor(Color.WHITE);
        clockText.setTextSize(36);
        clockText.setTypeface(Typeface.DEFAULT_BOLD);
        clockText.setGravity(Gravity.START);
        clockText.setPadding(0, dp(12), 0, dp(18));
        root.addView(clockText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView hint = new TextView(this);
        hint.setText("Toque em um app para abrir • segure para ver informações");
        hint.setTextColor(Color.argb(190, 255, 255, 255));
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, dp(10));
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(dp(14));
        grid.setHorizontalSpacing(dp(10));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setGravity(Gravity.CENTER);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(4), 0, dp(20));
        grid.setSelector(android.R.color.transparent);

        adapter = new AppsAdapter();
        grid.setAdapter(adapter);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openApp(apps.get(position));
            }
        });

        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                openAppSettings(apps.get(position));
                return true;
            }
        });

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        setContentView(root);
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolved = pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0));
        } else {
            resolved = pm.queryIntentActivities(intent, 0);
        }

        apps.clear();
        for (ResolveInfo info : resolved) {
            String packageName = info.activityInfo.packageName;
            if (packageName.equals(getPackageName())) continue;

            String label = info.loadLabel(pm).toString();
            Drawable icon = info.loadIcon(pm);
            apps.add(new AppInfo(label, packageName, icon));
        }

        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void openApp(AppInfo app) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launchIntent == null) {
            Toast.makeText(this, "Não consegui abrir: " + app.label, Toast.LENGTH_SHORT).show();
            return;
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
    }

    private void openAppSettings(AppInfo app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + app.packageName));
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class AppsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public Object getItem(int position) {
            return apps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout item;
            ImageView iconView;
            TextView labelView;

            if (convertView == null) {
                item = new LinearLayout(MainActivity.this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(dp(4), dp(8), dp(4), dp(8));

                iconView = new ImageView(MainActivity.this);
                iconView.setId(1001);
                iconView.setAdjustViewBounds(true);
                item.addView(iconView, new LinearLayout.LayoutParams(dp(52), dp(52)));

                labelView = new TextView(MainActivity.this);
                labelView.setId(1002);
                labelView.setTextColor(Color.WHITE);
                labelView.setTextSize(11);
                labelView.setGravity(Gravity.CENTER);
                labelView.setMaxLines(2);
                labelView.setPadding(0, dp(6), 0, 0);
                item.addView(labelView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
            } else {
                item = (LinearLayout) convertView;
            }

            AppInfo app = apps.get(position);
            iconView = item.findViewById(1001);
            labelView = item.findViewById(1002);
            iconView.setImageDrawable(app.icon);
            labelView.setText(app.label);

            return item;
        }
    }

    private static class AppInfo {
        final String label;
        final String packageName;
        final Drawable icon;

        AppInfo(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }
}
