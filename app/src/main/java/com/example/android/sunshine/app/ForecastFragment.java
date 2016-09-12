package com.example.android.sunshine.app;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh){
            FetchWeatherTask weatherTask = new FetchWeatherTask();
//            weatherTask.execute("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&APPID=7818dafeae1d8380a02ab5806ab021c7");
            weatherTask.execute("94043");
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            //get postal code from the user
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //dummy data for forecast app
        ArrayList<String> forecastDummyData = new ArrayList<>();
        forecastDummyData.add("Today - Sunny - 88 / 63");
        forecastDummyData.add("Tomorrow - Foggy - 70 / 46");
        forecastDummyData.add("Weds - Cloudy - 72 / 63");
        forecastDummyData.add("Thurs - Rainy - 64 / 51");
        forecastDummyData.add("Fri - Foggy - 70 / 46");
        forecastDummyData.add("Sat - Sunny - 76 / 68");

        //creating an adapter for the listview
        ArrayAdapter forecastDataAdapter = new ArrayAdapter(getActivity(), R.layout.list_item_forecast,
                R.id.list_item_forecast_textview, forecastDummyData);

        //attach adapter to listview
        ListView forecastListView = (ListView)rootView.findViewById(R.id.listview_forecast);
        forecastListView.setAdapter(forecastDataAdapter);

//        String owmAPIURL = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
//        new FetchWeatherTask().execute(owmAPIURL);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, Integer>{

        public String TAG_NAME = FetchWeatherTask.class.getSimpleName();

        @Override
        protected Integer doInBackground(String... params) {

            //check if param is passed
            if (params.length == 0){
                return null;
            }

            //Fetching forecast information from OpenWeatherMap API
            HttpURLConnection owmURLConnection = null;
            BufferedReader owmInputReader = null;

            String forecastJsonString = null;
            String format = "json";
            String units = "metric";
            int numDays = 7;
            String appID = "7818dafeae1d8380a02ab5806ab021c7";
//            weatherTask.execute("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&APPID=7818dafeae1d8380a02ab5806ab021c7");

            try{

                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt" ;
                final String APPID_PARAM = "APPID";


                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, String.valueOf(numDays))
                        .appendQueryParameter(APPID_PARAM, appID).build();


                URL owmURL = new URL(builtUri.toString());

                Log.v(TAG_NAME, "Built URI:" + builtUri.toString());

                owmURLConnection = (HttpURLConnection)owmURL.openConnection();
                owmURLConnection.setRequestMethod("GET");
                owmURLConnection.connect();

                //read data
                InputStream owmInputStream = owmURLConnection.getInputStream();

                if (owmInputStream == null) {
                    return null;
                }

                owmInputReader = new BufferedReader(new InputStreamReader(owmInputStream));

                String line;
                StringBuffer buffer = new StringBuffer();
                while((line = owmInputReader.readLine())!= null){
                    buffer.append(line+"\n");
                }

                if (buffer.length() == 0){
                    return null;
                }

                forecastJsonString = buffer.toString();
                Log.d("API reply",forecastJsonString);
                owmInputStream.toString();
            }catch (IOException e){
                Log.e(TAG_NAME, "Error", e);
                return null;
            }finally {
                //cleaning up connections
                if (owmURLConnection != null){
                    owmURLConnection.disconnect();
                }
                if (owmInputReader != null){
                    try{
                        owmInputReader.close();
                    }catch (IOException e){
                        Log.e(TAG_NAME, "Error closing stream", e);
                    }
                }
            }
            return null;
        }
    }
}
