package com.udacity.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by VutkaBilai on 4/15/17.
 * mail : la4508@gmail.com
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteService extends RemoteViewsService {

    public final String LOG_TAG = getClass().getSimpleName();


    private final DecimalFormat dollarFormatWithPlus;
    private final DecimalFormat dollarFormat;
    private final DecimalFormat percentageFormat;

    public DetailWidgetRemoteService() {
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {

                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(Contract.Quote.URI
                        , Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                        null, null,
                        Contract.Quote.COLUMN_SYMBOL);
                Log.d(getClass().getSimpleName() , "data "+data.getCount());
                Binder.restoreCallingIdentity(identityToken);

            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return (data == null) ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);


                //extract data for the top most stock symbol
                String stockSymbol = data.getString(Contract.Quote.POSITION_SYMBOL);
                String stockPrice = dollarFormat.format(data.getFloat(Contract.Quote.POSITION_PRICE));

                Log.d(getClass().getSimpleName(), "stock detail" + stockSymbol);

                float rawAbsoluteChange = data.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = data.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                boolean isGreen;

                isGreen = (rawAbsoluteChange > 0) ? true : false;

                String change = dollarFormatWithPlus.format(rawAbsoluteChange);
                String percentage = percentageFormat.format(percentageChange / 100);

                boolean isChangeWithPlus;

                if (PrefUtils.getDisplayMode(DetailWidgetRemoteService.this)
                        .equals(getString(R.string.pref_display_mode_absolute_key))) {
                    isChangeWithPlus = true;
                } else {
                    isChangeWithPlus = false;
                }


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

                final Intent fillInIntent = new Intent();


                fillInIntent.putExtra(Contract.Quote.COLUMN_SYMBOL , stockSymbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.top_stock_widget_large);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(Contract.Quote.POSITION_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
