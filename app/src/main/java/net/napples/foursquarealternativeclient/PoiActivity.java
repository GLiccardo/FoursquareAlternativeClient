package net.napples.foursquarealternativeclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

public class PoiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi);

        // Get Views reference
        ImageView poiImageView = (ImageView) findViewById(R.id.poi_imageview);
        TextView poiTextview = (TextView) findViewById(R.id.poi_textview);

        // Recupero il JSON
        Intent intent = getIntent();
        String messageJson = intent.getStringExtra("json_message");
        String messagePoiName = intent.getStringExtra("poi_name");

        // Mostro il JSON
        poiTextview.setText(messagePoiName + "\n" + messageJson);


    }
}
