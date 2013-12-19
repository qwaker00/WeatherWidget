package by.bsu.weatherwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by grickevich on 12/18/13.
 */
public class ShowAllActivity extends Activity {
    final static String LOG_TAG = "WeatherWidget";
    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;

    final int MENU_SETTINGS = 1;
    final int MENU_UPDATE = 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SETTINGS, 0, "Настройки");
        menu.add(0, MENU_UPDATE, 0, "Обновить");
        return super.onCreateOptionsMenu(menu);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "Create ShowAll");
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // и проверяем его корректность
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_CANCELED, resultValue);
        setTitle(R.string.weather);
        setContentView(R.layout.showall);

        try {
            SharedPreferences sp = getSharedPreferences(ConfigActivity.WIDGET_PREF, MODE_PRIVATE);
            String weather = sp.getString(MyWidget.WIDGET_WEATHER_DATA + widgetID, null);
            if (weather != null) {
                String[] data = weather.split("[|]");
                Weather[] weatherData = new Weather[data.length];
                for (int i = 0; i < data.length; ++i) {
                    Log.d(LOG_TAG, data[i]);

                    String[] parts = data[i].split("[#]");
                    if (parts.length != 5) continue;

                    if (parts[1].charAt(0) != '-' && parts[1].charAt(0) != '+' && !parts[1].equals("0")) parts[1] = "+" + parts[1];
                    if (parts[3].charAt(0) != '-' && parts[3].charAt(0) != '+' && !parts[3].equals("0")) parts[3] = "+" + parts[3];
                    parts[2] = parts[2].replace("+", "plus_").replace("-", "minus_");
                    parts[4] = parts[4].replace("+", "plus_").replace("-", "minus_");


                    weatherData[i] = new Weather(
                            parts[0],
                            getResources().getIdentifier(parts[2], "drawable", getPackageName()),
                            parts[1],
                            getResources().getIdentifier(parts[4], "drawable", getPackageName()),
                            parts[3]
                    );

                    weatherData[i].today = (i == 0);
                }

                ListView lv = (ListView)findViewById(R.id.listView);
                WeatherAdapter adapter = new WeatherAdapter(this,
                        R.layout.listview_item,
                        weatherData);
                lv.setAdapter(adapter);
            } else
                Log.d(LOG_TAG, "WeatherNull");
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error:", e);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == MENU_UPDATE) {
                Log.d(LOG_TAG, "Try update");
                setResult(RESULT_OK, resultValue);
                MyWidget.initiateUpdate(this, widgetID);
                finish();
            } else
            if (item.getItemId() == MENU_SETTINGS) {
                Log.d(LOG_TAG, "Try settings");
                Intent configIntent = new Intent(this, ConfigActivity.class);
                configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
                startActivity(configIntent);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error: ", e);
        }
        return super.onOptionsItemSelected(item);
    }
}


class Weather {
    int day_icon;
    String day_title;
    int night_icon;
    String night_title;
    String title;

    boolean today;

    public Weather(String title, int day_icon, String day_title, int night_icon, String night_title) {
        this.title = title;
        this.day_icon = day_icon;
        this.day_title = day_title;
        this.night_icon = night_icon;
        this.night_title = night_title;
    }
}


class WeatherAdapter extends ArrayAdapter<Weather> {
    Context context;
    int layoutResourceId;
    Weather data[] = null;

    static String[] monthName = {
            "января",
            "февряля",
            "марта",
            "апреля",
            "май",
            "июня",
            "июля",
            "августа",
            "сентября",
            "октября",
            "ноября",
            "декабря",
    };

    public WeatherAdapter(Context context, int layoutResourceId, Weather[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        try {
            WeatherHolder holder = null;

            if(row == null)
            {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new WeatherHolder();
                holder.day_icon = (ImageView)row.findViewById(R.id.item_icon_day);
                holder.day_text = (TextView)row.findViewById(R.id.item_text_day);
                holder.night_icon = (ImageView)row.findViewById(R.id.item_icon_night);
                holder.night_text = (TextView)row.findViewById(R.id.item_text_night);
                holder.day = (TextView)row.findViewById(R.id.item_date);
                holder.month = (TextView)row.findViewById(R.id.item_month);
                holder.dow = (TextView)row.findViewById(R.id.item_day_of_week);

                row.setTag(holder);
            }
            else
            {
                holder = (WeatherHolder)row.getTag();
            }

            Weather weather = data[position];

            Calendar c = Calendar.getInstance();
            try {
                c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(weather.title));
            } catch (Exception e) {
                Log.d(ShowAllActivity.LOG_TAG, "ParseError: ", e);
            }

            holder.day_text.setText(weather.day_title);
            holder.day_icon.setImageResource(weather.day_icon);
            holder.night_text.setText(weather.night_title);
            holder.night_icon.setImageResource(weather.night_icon);
            holder.day.setText("" + c.get(Calendar.DAY_OF_MONTH));
            holder.month.setText(c.getDisplayName(Calendar.MONTH, Calendar.ALL_STYLES, Locale.getDefault()));
            holder.dow.setText(c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.ALL_STYLES, Locale.getDefault()));

        } catch (Exception e) {
            Log.d(ShowAllActivity.LOG_TAG, "Error: ", e);
        }
        return row;
    }

    static class WeatherHolder
    {
        ImageView day_icon;
        TextView day_text;
        ImageView night_icon;
        TextView night_text;
        TextView day;
        TextView month;
        TextView dow;
    }
}