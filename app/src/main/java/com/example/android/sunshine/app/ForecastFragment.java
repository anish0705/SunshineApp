package com.example.android.sunshine.app;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
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
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute("94043");

        ArrayAdapter forecastDataAdapter = new ArrayAdapter(getActivity(), R.layout.list_item_forecast,
                R.id.list_item_forecast_textview, forecastDummyData);

        //attach adapter to listview
        ListView forecastListView = (ListView)rootView.findViewById(R.id.listview_forecast);
        forecastListView.setAdapter(forecastDataAdapter);

//        String owmAPIURL = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
//        new FetchWeatherTask().execute(owmAPIURL);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        public String TAG_NAME = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
            Log.v(TAG_NAME, "Forecast entry: " + s);
            }
            return resultStrs;

        }


        @Override
        protected String[] doInBackground(String... params) {

            //check if param is passed
            if (params.length == 0) {
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

            try {

                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";


                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, String.valueOf(numDays))
                        .appendQueryParameter(APPID_PARAM, appID).build();


                URL owmURL = new URL(builtUri.toString());

                Log.v(TAG_NAME, "Built URI:" + builtUri.toString());

                owmURLConnection = (HttpURLConnection) owmURL.openConnection();
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
                while ((line = owmInputReader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                forecastJsonString = buffer.toString();
                Log.d("API reply", forecastJsonString);
                owmInputStream.toString();
            } catch (IOException e) {
                Log.e(TAG_NAME, "Error", e);
                return null;
            } finally {
                //cleaning up connections
                if (owmURLConnection != null) {
                    owmURLConnection.disconnect();
                }
                if (owmInputReader != null) {
                    try {
                        owmInputReader.close();
                    } catch (IOException e) {
                        Log.e(TAG_NAME, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonString, numDays);
            } catch (JSONException e) {
                Log.e(TAG_NAME, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }
    }
}
