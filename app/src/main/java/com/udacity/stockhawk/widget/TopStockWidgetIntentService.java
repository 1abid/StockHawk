package com.udacity.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.MainActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by VutkaBilai on 4/13/17.
 * mail : la4508@gmail.com
 */

public class TopStockWidgetIntentService extends IntentService {

    private final DecimalFormat dollarFormatWithPlus;
    private final DecimalFormat dollarFormat;
    private final DecimalFormat percentageFormat;

    private static final String[] STOCK_COLUMNS = {
            Contract.Quote.COLUMN_SYMBOL,
            Contract.Quote.COLUMN_PRICE,
            Contract.Quote.COLUMN_ABSOLUTE_CHANGE,
            Contract.Quote.COLUMN_PERCENTAGE_CHANGE
    };


    public static final int POSITION_SYMBOL = 0;
    public static final int POSITION_PRICE = 1;
    public static final int POSITION_ABSOLUTE_CHANGE = 2;
    public static final int POSITION_PERCENTAGE_CHANGE = 3;


    public TopStockWidgetIntentService(){
        super(TopStockWidgetIntentService.class.getSimpleName());

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                TopStockWidget.class));


        Uri contentQuoteUri = Contract.Quote.URI ;

        contentQuoteUri = contentQuoteUri.buildUpon().appendQueryParameter(Contract.Quote.COLUMN_PRICE , "MAX(price)").build();
        Log.d(getClass().getSimpleName() , "uri "+contentQuoteUri.toString());


        Cursor data = getContentResolver().query(contentQuoteUri,STOCK_COLUMNS,null,null,null);

        if(data == null){
            return;
        }
        if(!data.moveToFirst()){
            data.close();
            Log.d(getClass().getSimpleName() , "no result");
            return;
        }

        //extract data for the top most stock symbol
        String stockSymbol = data.getString(POSITION_SYMBOL);
        String stockPrice = dollarFormat.format(data.getFloat(POSITION_PRICE));

        Log.d(getClass().getSimpleName() , "stock "+stockSymbol);

        float rawAbsoluteChange = data.getFloat(POSITION_ABSOLUTE_CHANGE);
        float percentageChange = data.getFloat(POSITION_PERCENTAGE_CHANGE);

        boolean isGreen ;

        isGreen =(rawAbsoluteChange>0) ? true : false;

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormat.format(percentageChange / 100);

        boolean isChangeWithPlus ;

        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            isChangeWithPlus = true ;
        } else {
            isChangeWithPlus = false ;
        }



        for (int appWidgetId : appWidgetIds) {
            // Find the correct layout based on the widget's width
            int widgetWidth = getWidgetWidth(appWidgetManager , appWidgetId);
            int defaultWidth = getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
            int largeWidth = getResources().getDimensionPixelSize(R.dimen.widget_today_large_width);
            int layoutId;
            if(widgetWidth >= largeWidth)
                layoutId = R.layout.top_stock_widget_large;
            else if(widgetWidth >= defaultWidth)
                layoutId = R.layout.top_stock_widget;
            else
                layoutId = R.layout.top_stock_widget_small;


            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            views.setTextViewText(R.id.widget_stock_price , stockPrice);
            views.setTextViewText(R.id.widget_stock_symbol , stockSymbol);
            if(isGreen)
                views.setInt(R.id.widget_stock_change, "setBackgroundResource" , R.drawable.percent_change_pill_green);
            else
                views.setInt(R.id.widget_stock_change, "setBackgroundResource" , R.drawable.percent_change_pill_red);

            if(isChangeWithPlus)
                views.setTextViewText(R.id.widget_stock_change , change);
            else
                views.setTextViewText(R.id.widget_stock_change , percentage);


            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            launchIntent.putExtra(Contract.Quote.COLUMN_SYMBOL , stockSymbol);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }


    private int getWidgetWidth(AppWidgetManager appWidgetManager, int appWidgetId) {
        // Prior to Jelly Bean, widgets were always their default size
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
        }
        // For Jelly Bean and higher devices, widgets can be resized - the current size can be
        // retrieved from the newly added App Widget Options
        return getWidgetWidthFromOptions(appWidgetManager, appWidgetId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getWidgetWidthFromOptions(AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            int minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            // The width returned is in dp, but we'll convert it to pixels to match the other widths
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidthDp,
                    displayMetrics);
        }
        return  getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_stock_price, description);
    }
}
