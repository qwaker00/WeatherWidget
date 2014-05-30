package by.bsu.weatherwidget;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by grickevich on 12/10/13.
 */
public class MyWidget extends AppWidgetProvider {

    final static String LOG_TAG = "WeatherWidget";
    final static String UPDATE_ALL_WIDGETS = "update_all_widgets";
    final static String WIDGET_WEATHER_DATA = "widget_weather_data";
    final static int UPDATE_INTERVAL = 900;

    //Map<String, String> cityCode;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        Intent intent = new Intent(context, MyWidget.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), UPDATE_INTERVAL * 1000, pIntent);

        //cityCode = new Hashtable<String, String>();
        //cityCode.put("minsk", "26850");

        Log.d(LOG_TAG, "onEnabled");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Log.d(LOG_TAG, "onUpdateStart " + Arrays.toString(appWidgetIds));
        SharedPreferences sp = context.getSharedPreferences(ConfigActivity.WIDGET_PREF, Context.MODE_PRIVATE);
        for (int id : appWidgetIds) {
            try {
                Log.d(LOG_TAG, " UpdateId " + id);
                updateWidget(context, appWidgetManager, sp, id);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Error: ", e);
            }
        }

        Log.d(LOG_TAG, "onUpdateFinish " + Arrays.toString(appWidgetIds));
    }

    static final Pattern temperaturePattern = Pattern.compile(".*<temperature.*color=\"([\\w\\d]+)\".*>(.+)</temperature>.*");
    static final Pattern temperatureTomorrowPattern = Pattern.compile(".*<temperature.*color=\"[\\w\\d]+\".*type=\"tomorrow\".*>(.+)</temperature>.*");
    static final Pattern temperatureNightPattern = Pattern.compile(".*<temperature.*color=\"[\\w\\d]+\".*type=\"night\".*>(.+)</temperature>.*");
    static final Pattern simpleTemperaturePattern = Pattern.compile(".*<temperature>(.+)</temperature>.*");
    static final Pattern weatherTypePattern = Pattern.compile(".*<weather_type>(    .+)</weather_type>.*");
    static final Pattern todayImagePattern = Pattern.compile(".*<image-v3.*>(.+)</image-v3>.*");
    static final Pattern observationPattern = Pattern.compile(".*<observation_time>(.+)</observation_time>.*");
    static final Pattern startDayPattern = Pattern.compile(".*<day date=\"(.*)\">.*");
    static final Pattern endDayPattern = Pattern.compile(".*</day>.*");
    static final Pattern dayShortPattern = Pattern.compile(".*<day_part.*type=\"day_short\">.*");
    static final Pattern nightShortPattern = Pattern.compile(".*<day_part.*type=\"night_short\">.*");
    static final Pattern dayPartEndPattern = Pattern.compile(".*</day_part>.*");



    static void initiateUpdate(Context context, int widgetID) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        MyWidget.updateWidget(context, appWidgetManager, context.getSharedPreferences(ConfigActivity.WIDGET_PREF, ConfigActivity.MODE_PRIVATE), widgetID);
        Toast.makeText(context, "Обновление данных...", Toast.LENGTH_SHORT).show();
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, SharedPreferences sp, int id) {
        TUpdateTaskData data = new TUpdateTaskData();
        data.widgetId = id;
        data.widgetManager = appWidgetManager;
        data.widget = new RemoteViews(context.getPackageName(), R.layout.widget);
        //data.cityId = "26063"; // Piter
        data.cityId = "26850"; // Minsk
        data.prefs = sp;
        data.context = context;

        Intent configIntent = new Intent(data.context, ShowAllActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,  data.widgetId);
        PendingIntent pIntent = PendingIntent.getActivity(data.context, data.widgetId, configIntent, 0);
        data.widget.setOnClickPendingIntent(R.id.mainframe, pIntent);

        String bgColorPref = sp.getString(ConfigActivity.WIDGET_BG_COLOR + id, null);
        if (bgColorPref != null) {
            if (bgColorPref.equals("weather")) {
                data.widget.setInt(R.id.mainframe, "setBackgroundColor", Color.parseColor("#f0f0f0"));
            } else
            if (bgColorPref.equals("transparent")) {
                data.widget.setInt(R.id.mainframe, "setBackgroundColor", Color.TRANSPARENT);
            } else
            if (bgColorPref.equals("black")) {
                data.widget.setInt(R.id.mainframe, "setBackgroundColor", Color.BLACK);
            }
        }

        int fgColor = Color.GRAY;
        String fgColorPref = sp.getString(ConfigActivity.WIDGET_FG_COLOR + id, null);
        if (fgColorPref != null) {
            if (fgColorPref.equals("white")) {
                fgColor = Color.WHITE;
            } else
            if (fgColorPref.equals("black")) {
                fgColor = Color.BLACK;
            }
        }
        data.widget.setTextColor(R.id.tempetature_text, fgColor);
        data.widget.setTextColor(R.id.weather_type_text, fgColor);
        data.widget.setTextColor(R.id.update_time_text, fgColor);
        data.widget.setTextColor(R.id.temperature_tomorrow_text, fgColor);
        data.widget.setTextColor(R.id.tomorrow_text, fgColor);
        data.widget.setTextColor(R.id.temperature_night_text, fgColor);
        data.widget.setTextColor(R.id.night_text, fgColor);
        data.widget.setTextColor(R.id.at_text, fgColor);
        data.widget.setTextColor(R.id.city_text, fgColor);

        data.widget.setTextViewText(R.id.city_text, "Минск");

        data.widgetManager.updateAppWidget(data.widgetId, data.widget);

        new AsyncTask<TUpdateTaskData, Void, TParsedData>() {
            @Override
            protected TParsedData doInBackground(TUpdateTaskData... strings) {
                for (TUpdateTaskData s : strings) {
                    try {
                        Log.d(LOG_TAG, "try!");

                        TParsedData result = new TParsedData();
                        URL url = new URL("http://195.50.18.124:8181/");
                        URLConnection conn = url.openConnection();
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(10000);

                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line;
                        StringBuilder sb = new StringBuilder();
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }

                        result.data = s;
                        JSONObject o = new JSONObject(sb.toString());
                        JSONObject current = o.getJSONObject("current");
                        JSONArray forecast = o.getJSONArray("forecast");
                        JSONObject info = o.getJSONObject("info");

                        result.temperature = current.getString("temperature");
                        result.weatherType = current.getString("weather_type");
                        result.todayImage = current.getString("image");
                        result.observationTime = current.getLong("uptime");

                        result.temperatureTomorrow = info.getString("tomorrow");
                        result.temperatureNight = info.getString("night");

                        StringBuilder dates = new StringBuilder();
                        for (int i = 0; i < forecast.length(); ++i) {
                            JSONObject day = forecast.getJSONObject(i);
                            dates.append(day.getString("date"));
                            dates.append('#');
                            dates.append(day.getJSONObject("day").getString("temperature"));
                            dates.append('#');
                            dates.append(day.getJSONObject("day").getString("image"));
                            dates.append('#');
                            dates.append(day.getJSONObject("night").getString("temperature"));
                            dates.append('#');
                            dates.append(day.getJSONObject("night").getString("image"));
                            dates.append('|');
                        }

                        Log.d(LOG_TAG, "Parsed data: " + dates.toString());
                        s.prefs.edit().putString(WIDGET_WEATHER_DATA + s.widgetId, dates.toString()).commit();

                        Log.d(LOG_TAG, "parsed!");

                        if (result.todayImage == null) return null;
                        result.todayImage = result.todayImage.replace("+", "plus_").replace("-", "minus_");

                        Log.d(LOG_TAG, "OK!");
                        return result;
                    } catch (Exception e) {
                        Log.d(LOG_TAG, "Error!", e);
                        return null;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(TParsedData r) {
                if (r != null) {
                    try {
                        r.data.widget.setInt(R.id.form, "setVisibility", View.VISIBLE);
                        r.data.widget.setInt(R.id.loading_text, "setVisibility", View.GONE);

                        Log.d(LOG_TAG, "Good result");
                        r.data.widget.setImageViewResource(R.id.weather_type_image, r.data.context.getResources().getIdentifier(r.todayImage, "drawable", r.data.context.getPackageName()));

                        r.data.widget.setTextViewText(R.id.weather_type_text, r.weatherType);

                        if (r.temperature.charAt(0) != '-' && r.temperature.charAt(0) != '+' && !r.temperature.equals("0")) r.temperature = "+" + r.temperature;
                        r.data.widget.setTextViewText(R.id.tempetature_text, r.temperature + "°C");

                        if (r.temperatureTomorrow.charAt(0) != '-' && r.temperatureTomorrow.charAt(0) != '+' && !r.temperatureTomorrow.equals("0")) r.temperatureTomorrow = "+" + r.temperatureTomorrow;
                        r.data.widget.setTextViewText(R.id.temperature_tomorrow_text, r.temperatureTomorrow + "°C");

                        if (r.temperatureNight.charAt(0) != '-' && r.temperatureNight.charAt(0) != '+' && !r.temperatureTomorrow.equals("0")) r.temperatureNight = "+" + r.temperatureNight;
                        r.data.widget.setTextViewText(R.id.temperature_night_text, r.temperatureNight + "°C");

                        Calendar uptime = Calendar.getInstance();
                        uptime.setTimeInMillis(r.observationTime * 1000);
                        r.data.widget.setTextViewText(R.id.update_time_text,
                                String.format("%02d.%02d.%04d %02d:%02d",
                                                  uptime.get(Calendar.DAY_OF_MONTH),
                                                  uptime.get(Calendar.MONTH),
                                                  uptime.get(Calendar.YEAR),
                                                  uptime.get(Calendar.HOUR_OF_DAY),
                                                  uptime.get(Calendar.MINUTE)
                                )
                        );

                        r.data.widgetManager.updateAppWidget(r.data.widgetId, r.data.widget);
                    } catch (Exception e) {
                        Log.d(LOG_TAG, "Error!", e);
                    }
                } else
                    Log.d(LOG_TAG, "Null result..");
            }
        }.execute(data);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d(LOG_TAG, "onDeleted " + Arrays.toString(appWidgetIds));

        SharedPreferences.Editor editor = context.getSharedPreferences(ConfigActivity.WIDGET_PREF, Context.MODE_PRIVATE).edit();
        for (int widgetID : appWidgetIds) {
            editor.remove(ConfigActivity.WIDGET_BG_COLOR + widgetID);
            editor.remove(ConfigActivity.WIDGET_FG_COLOR + widgetID);
            editor.remove(WIDGET_WEATHER_DATA + widgetID);
        }
        editor.commit();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        Intent intent = new Intent(context, MyWidget.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);

        Log.d(LOG_TAG, "onDisabled");
    }

    @Override
    public void onReceive(Context context , Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction() != null && intent.getAction().equalsIgnoreCase(UPDATE_ALL_WIDGETS)) {
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager == null) return;
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, ids);
        }
    }
}

class TUpdateTaskData {
    RemoteViews         widget;
    AppWidgetManager    widgetManager;
    int                 widgetId;
    String              cityId;
    SharedPreferences   prefs;
    Context             context;
}

class TParsedData {
    String              todayImage;
    String              temperature;
    String              temperatureTomorrow;
    String              temperatureNight;
    String              weatherType;
    long                observationTime;
    TUpdateTaskData     data;
}
