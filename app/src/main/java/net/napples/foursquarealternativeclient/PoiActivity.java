package net.napples.foursquarealternativeclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PoiActivity extends AppCompatActivity {

    private ArrayList<FoursquareVenue> venuesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi);

        // Get Views reference
        ImageView poiImageView = (ImageView) findViewById(R.id.poi_imageview);
        TextView poiNomeTextview = (TextView) findViewById(R.id.poi_nome);
        TextView poiIndirizzoTextview = (TextView) findViewById(R.id.poi_indirizzo);
        TextView poiDescrizioneTextview = (TextView) findViewById(R.id.poi_descrizione);
        TextView poiCheckinTextview = (TextView) findViewById(R.id.poi_checkin);

        // Recupero il JSON
        Intent intent = getIntent();
        String messageJson = intent.getStringExtra("json_message");
        String messagePoiName = intent.getStringExtra("poi_name");

        // Parsing JSON
        venuesList = (ArrayList<FoursquareVenue>) parseFoursquare(messageJson);

        // Costruisco la lista di oggetti da visualizzare nella ListView
        List<String> listTitle = new ArrayList<String>();

        for (int i = 0; i < venuesList.size(); i++) {

            if (venuesList.get(i).getName().equals(messagePoiName)) {
                poiNomeTextview.setText(venuesList.get(i).getName());
                poiIndirizzoTextview.setText(venuesList.get(i).getAddress() + ", " + venuesList.get(i).getCity());
                poiDescrizioneTextview.setText(venuesList.get(i).getCategory());
                poiCheckinTextview.setText(venuesList.get(i).getCheckin());
            }

        }

    }



    private static ArrayList<FoursquareVenue> parseFoursquare(final String response) {

        ArrayList<FoursquareVenue> temp = new ArrayList<FoursquareVenue>();
        try {

            // Creo un jsonObject in modo da fare il parsing della response della chiamata HTTP
            JSONObject jsonObject = new JSONObject(response);

            // make an jsonObject in order to parse the response
            if (jsonObject.has("response")) {
                if (jsonObject.getJSONObject("response").has("venues")) {
                    JSONArray jsonArray = jsonObject.getJSONObject("response").getJSONArray("venues");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        FoursquareVenue poi = new FoursquareVenue();

                        if (jsonArray.getJSONObject(i).has("name")) {
                            poi.setName(jsonArray.getJSONObject(i).getString("name"));

                            if (jsonArray.getJSONObject(i).has("location")) {
                                if (jsonArray.getJSONObject(i).getJSONObject("location").has("address")) {
                                        poi.setAddress(jsonArray.getJSONObject(i).getJSONObject("location").getString("address"));
                                }
                                if (jsonArray.getJSONObject(i).getJSONObject("location").has("city")) {
                                    poi.setCity(jsonArray.getJSONObject(i).getJSONObject("location").getString("city"));
                                }
                            }

                            if (jsonArray.getJSONObject(i).has("categories")) {
                                if (jsonArray.getJSONObject(i).getJSONArray("categories").length() > 0) {
                                    if (jsonArray.getJSONObject(i).getJSONArray("categories").getJSONObject(0).has("name")) {
                                        poi.setCategory(jsonArray.getJSONObject(i).getJSONArray("categories").getJSONObject(0).getString("name"));
                                    }
                                }
                            }

                            if (jsonArray.getJSONObject(i).has("stats")) {
                                if (jsonArray.getJSONObject(i).getJSONObject("stats").has("checkinsCount")) {
                                    poi.setCheckin(jsonArray.getJSONObject(i).getJSONObject("stats").getString("checkinsCount"));
                                }
                            }

                            temp.add(poi);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<FoursquareVenue>();
        }
        return temp;

    }
}
