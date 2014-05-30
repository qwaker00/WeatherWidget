package by.bsu.weatherwidget;

/**
 * Created by grickevich on 12/12/13.
 */
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class ConfigActivity extends Activity {

    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;

    final String LOG_TAG = "WeatherWidget";

    public final static String WIDGET_PREF = "widget_pref";
    public final static String WIDGET_BG_COLOR = "widget_bg_color_";
    public final static String WIDGET_FG_COLOR = "widget_fg_color_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate config");

        // извлекаем ID конфигурируемого виджета
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // и проверяем его корректность
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        // формируем intent ответа
        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        // отрицательный ответ
        setResult(RESULT_CANCELED, resultValue);
        setTitle(R.string.config);
        setContentView(R.layout.config);

        try {
            SharedPreferences sp = getSharedPreferences(WIDGET_PREF, MODE_PRIVATE);

            String bgColor = sp.getString(WIDGET_BG_COLOR + widgetID, null);
            Log.d(LOG_TAG, "Bg: " + bgColor);
            if (bgColor != null) {
                RadioGroup rg = (RadioGroup)findViewById(R.id.bg_radio);
                if (bgColor.equals("white"))
                    rg.check(R.id.bg_radio_white);
                else if (bgColor.equals("transparent"))
                    rg.check(R.id.bg_radio_transparent);
                else if (bgColor.equals("black"))
                    rg.check(R.id.bg_radio_black);
            }

            String fgColor = sp.getString(WIDGET_FG_COLOR + widgetID, null);
            Log.d(LOG_TAG, "Fg: " + fgColor);
            if (fgColor != null) {
                RadioGroup rg = (RadioGroup)findViewById(R.id.fg_radio);
                if (fgColor.equals("white"))
                    rg.check(R.id.fg_radio_white);
                else if (fgColor.equals("black"))
                    rg.check(R.id.fg_radio_black);
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "exception", e);
        }
    }

    public void onClick(View v) {
        int selBGColor = ((RadioGroup)findViewById(R.id.bg_radio)).getCheckedRadioButtonId();
        String bgColor = null;
        switch (selBGColor) {
            case R.id.bg_radio_transparent:
                bgColor = "transparent";
                break;
            case R.id.bg_radio_white:
                bgColor = "white";
                break;
            case R.id.bg_radio_black:
                bgColor = "black";
                break;
        }

        int selFGColor = ((RadioGroup) findViewById(R.id.fg_radio)).getCheckedRadioButtonId();
        String fgColor = null;
        switch (selFGColor) {
            case R.id.fg_radio_white:
                fgColor = "white";
                break;
            case R.id.fg_radio_black:
                fgColor = "black";
                break;
        }

        Log.d(LOG_TAG, "FG: " + fgColor);

        // Записываем значения с экрана в Preferences
        SharedPreferences sp = getSharedPreferences(WIDGET_PREF, MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(WIDGET_BG_COLOR + widgetID, bgColor);
        editor.putString(WIDGET_FG_COLOR + widgetID, fgColor);
        editor.commit();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        MyWidget.updateWidget(this, appWidgetManager, sp, widgetID);

        // положительный ответ
        setResult(RESULT_OK, resultValue);

        Log.d(LOG_TAG, "finish config " + widgetID);
        finish();
    }
}
