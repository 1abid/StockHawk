package com.udacity.stockhawk.ui;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailStockActivity extends AppCompatActivity {


    @BindView(R.id.chart)
    BarChart chart;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_stock);

        ButterKnife.bind(this);

        setUpLineChart();

        Bundle b = getIntent().getExtras();

        if(b!=null)
            loadChartData(b.getString(Contract.Quote.COLUMN_SYMBOL));
    }


    private void setUpLineChart() {

        //setting up line chart

        // enable touch gestures
        chart.setTouchEnabled(true);

        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM HH:mm");

            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                long millis = TimeUnit.HOURS.toMillis((long) value);
                return mFormat.format(new Date(millis));
            }
        });


        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(500f);
        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }


    private void loadChartData(String symbol) {

        //now in minute
        long now = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());

        ContentResolver cotentResolver = getContentResolver();

        Uri contentUri = Contract.Quote.URI;
        contentUri = contentUri.buildUpon().appendPath(symbol).build();

        Cursor c = null ;
        float price = 0;

        try {
            c = cotentResolver.query(contentUri,
                    null
                    , null
                    , null,
                    null);

            if (c != null && c.moveToFirst())
                price = c.getFloat(c.getColumnIndex(Contract.Quote.COLUMN_PRICE)) ;

        }finally {
            c.close();
        }

        List<BarEntry> entries = new ArrayList<BarEntry>();
        entries.add(new BarEntry(now , price));


        // create a dataset and give it a type
        BarDataSet set1 = new BarDataSet(entries , symbol);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ColorTemplate.getHoloBlue());
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));

        // create a data object with the datasets
        BarData data = new BarData(set1);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);
        data.setBarWidth(0.9f); // set custom bar width
        chart.setFitBars(true);

        // set data
        chart.setData(data);
        chart.invalidate();
    }


    class loadlazyChart extends AsyncTask<Void , Void , String> {

        @Override
        protected String doInBackground(Void... params) {
            Uri contentQuoteUri = Contract.Quote.URI ;

            contentQuoteUri = contentQuoteUri.buildUpon().appendQueryParameter(Contract.Quote.COLUMN_PRICE , "MAX(price)").build();

            Cursor data = getContentResolver().query(contentQuoteUri,null,null,null,null);

            if(data == null){
                return "";
            }
            if(!data.moveToFirst()){
                data.close();
                Log.d(getClass().getSimpleName() , "no result");
                return "";
            }

            return data.getString(data.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            loadChartData(s);
        }
    }
}
