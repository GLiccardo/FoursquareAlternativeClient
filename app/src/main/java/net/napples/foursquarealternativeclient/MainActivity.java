package net.napples.foursquarealternativeclient;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "FoursquareClient";

    /* Foursquare App */
    private final String CLIENT_ID = "SUAEVJQGEQ5J2F2ZIUEL3DMR5R3IQIOEGR322NNIXSRZ5BNP";
    private final String CLIENT_SECRET = "EG3HNH3PZX1EP0USHV43JMO1241SF310KB1QUFOD531UTY2M";

    /* Dialog "Chooser" String */
    private static final String DIALOG_TITLE = "Attenzione!";
    private static final String DIALOG_MESSAGE = "L'applicazione ha bisogno di accedere alla tua posizione. Clicca su 'Consenti' per continuare";
    private static final String DIALOG_ACCEPT = "Consenti";
    private static final String DIALOG_CANCEL = "Rifiuta";

    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    /* Posizione */
    private static final int INTERVALLO_DI_TEMPO = 10000;  // 10s
    private static final int DISTANZA_MINIMA = 50;         // 50m
    private static final double FAKE_LONGITUDE = 40.911586d;
    private static final double FAKE_LATITUDE = 14.203376d;

    /* GPS */
    private String mProviderName;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLocation;

    /* Foursquare */
    private ArrayList<FoursquareVenue> venuesList;

    /* Views */
    private TextView mTextView;
    private ListView mListView;
    private ArrayAdapter<String> myAdapter;

    /* HTTP Request */
    OkHttpClient okHttpClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Entrato in onCreate()");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Entrato in onStart()");

        // Ottengo dei reference alle view
        findViewsById();

        // mostro il dialog iniziale ed ottengo le coordinate del dispositivo
        showDialog();



    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /*
     * Dialog mostrato non appena viene avviata l'applicazione.
     * Viene chiesto all'utente se vuole rendere nota la sua posizione
     * In caso positivo si prosegue con l'esecuzione dell'applicazione, altrimenti viene chiusa.
     */
    private void showDialog() {
        Log.d(TAG, "Entrato in showDialog()");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(DIALOG_TITLE);
        builder.setMessage(DIALOG_MESSAGE);

        builder.setPositiveButton(DIALOG_ACCEPT, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Ottengo le coordinate del dispositivo
                getUserPosition();
                // Faccio la richiesta HTTP a foursquare
                makeFoursquareRequest();
            }
        });
        builder.setNegativeButton(DIALOG_CANCEL, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Termino l'esecuzione dell'applicazione
                Toast.makeText(MainActivity.this, "L'app verrà chiusa entro pochi secondi", Toast.LENGTH_SHORT).show();

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            synchronized (this) {
                                wait(3000);
                            }
                        } catch (InterruptedException exc) {
                            exc.printStackTrace();
                        }

                        finish();
                        System.exit(0);
                    }
                };

                thread.start();
            }
        });

        builder.show();
    }



    /*
     * Uso LocationManager per ottenere le coordinate del dispositivo.
     * L'uso delle API 23 richiede la verifica a runtime dei permessi per la posizione, se i permessi
     * sono stati approvati verifico se c'è un provider attivo:
     *  - se non c'è, permetto all'utente di attivare il GPS
     *  - se c'è, recupero le coordinate (saranno in mLocation)
     */
    private void getUserPosition() {
        Log.d(TAG, "Entrato in getUserPosition()");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Verifico se uno dei provider è attivato o meno
        mProviderName = checkProvidersStatus();

        // API 23: richiede la verifica a runtime dei permessi ACCESS_FINE_LOCATION e ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Nessun provider attivo: si rimanda l'utente alla schermata per l'attivazione del GPS
            if (mProviderName == null || mProviderName.equals("")) {
                Toast.makeText(MainActivity.this, "Attiva il GPS per rintracciare la tua posizione", Toast.LENGTH_SHORT).show();
                Intent intentGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intentGPS);
            }

            double[] coordinates = new double[2];

            // Almeno un provider attivo: recupero le coordinate della posizione in base al tipo di provider
            switch (mProviderName) {
                case "passive":
                    // Si potrebbe personalizzare il get delle coordinate in base al tipo di provider
                case "network":

                case "gps":
                    mLocationManager.requestLocationUpdates(mProviderName, INTERVALLO_DI_TEMPO, DISTANZA_MINIMA, this);
                    mLocation = mLocationManager.getLastKnownLocation(mProviderName);

                    // Se non sono state ricavate le coordinate, ne imposto di fittizie
                    if (mLocation == null) {
                        mLocation = new Location("");
                        mLocation.setLongitude(FAKE_LONGITUDE);
                        mLocation.setLatitude(FAKE_LATITUDE);
                        Log.d(TAG, "Coordinate fittizie: " + mLocation.getLongitude() + " , " + mLocation.getLatitude());
                    }
                    else {
                        Log.d(TAG, "Coordinate emulatore: " + mLocation.getLongitude() + " , " + mLocation.getLatitude());
                    }
                    break;
            }

        // Uno o entrambi i permessi non sono stati approvati. Ne faccio richiesta
        } else {

            // The ACCESS_COARSE_LOCATION is denied, then I request it and manage the result in
            // onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSION_ACCESS_COARSE_LOCATION);
            }
            // The ACCESS_FINE_LOCATION is denied, then I request it and manage the result in
            // onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_ACCESS_FINE_LOCATION);
            }

        }

    }


    /*
     * Metodo che verifica se almeno uno dei provider è:
     *  - abilitato     <<  ritorna il nome del miglior provider (GPS, NETWORK e PASSIVE)
     *  - disabilitato  <<  ritorna null
     */
    private String checkProvidersStatus() {
        Log.d(TAG, "Entrato in checkProvidersStatus()");

        // Definisco i criteri per stabilire il miglior provider da usare per ottenere la posizione
        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationCriteria.setAltitudeRequired(false);
        locationCriteria.setBearingRequired(false);
        locationCriteria.setCostAllowed(true);
        locationCriteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

        // getBestProvider() ritorna il provider migliore
        if (mLocationManager.getBestProvider(locationCriteria, true) != null) {
            return mLocationManager.getBestProvider(locationCriteria, true);
        } else {
            return null;
        }

        /*
        // I provider sono 3 e in ordine di precisione sono: GPS, NETWORK e PASSIVE
        boolean gps_enabled = false;
        boolean network_enabled = false;
        boolean passive_enabled = false;

        // Variabili per verificare se i provider "GPS" e "NETWORK" sono abilitati
        gps_enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        passive_enabled = mLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
        */

    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(MainActivity.this, "onLocationChanged()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(MainActivity.this, "onProviderDisabled()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(MainActivity.this, "onProviderEnabled()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(MainActivity.this, "onStatusChanged()", Toast.LENGTH_SHORT).show();
    }



    private void makeFoursquareRequest() {
        Log.d(TAG, "Entrato in makeFoursquareRequest()");


        //String stringUrl = urlText.getText().toString();

        // Prima di fare la chiamata HTTP, verifico se la connessione è attiva
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.d(TAG, "Connessione attiva");
            new FoursquareCall().execute();
        } else {
            Log.d(TAG, "Connessione assente");
            Toast.makeText(MainActivity.this, "Connessione assente", Toast.LENGTH_SHORT).show();
        }

        /*
        okHttpClient = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://raw.github.com/square/okhttp/master/README.md")
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            Log.d("ANSWER", response.body().string());
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        */

    }


    private void findViewsById() {

        try {
            mTextView = (TextView) findViewById(R.id.textview);
            mListView = (ListView) findViewById(R.id.listview);
        } catch (NullPointerException exc) {
            exc.printStackTrace();
        }
    }



    private class FoursquareCall extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG, "AsyncTask: Entrato in doInBackground()");

            // Nella chiamata HTTP dal Search Venues è necessario passare client ID, client secret,
            // version parameter e coordinate. Ritornerà come risultato un JSON
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("https://api.foursquare.com/v2/venues/search?client_id=");
            stringBuilder.append(CLIENT_ID);
            stringBuilder.append("&client_secret=");
            stringBuilder.append(CLIENT_SECRET);
            stringBuilder.append("&v=20130815&ll=");
            stringBuilder.append(mLocation.getLongitude());
            stringBuilder.append(",");
            stringBuilder.append(mLocation.getLatitude());

            Log.d(TAG, "Chiamata HTTP StringBuilder1: " + stringBuilder);

            // TODO: Da eliminare
            // -----------------------------------------------------------
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("https://api.foursquare.com/v2/venues/search?client_id=" +
                    CLIENT_ID + "&client_secret=" + CLIENT_SECRET +
                    "&v=20130815&ll=40.7463956,-73.9852992");
            Log.d(TAG, "Chiamata HTTP StringBuilder2: " + stringBuilder2);
            // -----------------------------------------------------------

            // Implemento la chiamata HTTP usando la libreria OkHttp
            okHttpClient = new OkHttpClient();

            Request request = new Request.Builder()
                    //.url("https://raw.github.com/square/okhttp/master/README.md")
                    .url(String.valueOf(stringBuilder))
                    .build();

            try {
                Response response = okHttpClient.newCall(request).execute();
                return response.body().string(); // La response viene gestita nell'OnPostExecute

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        // onPostExecute mostra i risultati dell'AsyncTask.
        @Override
        protected void onPostExecute(final String result) {
            Log.d(TAG, "AsyncTask: Entrato in onPostExecute()");

            // TODO: Da eliminare
            //mTextView.setText(result);

            venuesList = (ArrayList<FoursquareVenue>) parseFoursquare(result);

            // Costruisco la lista di oggetti da visualizzare nella ListView
            List<String> listTitle = new ArrayList<String>();

            for (int i = 0; i < venuesList.size(); i++) {
                // Creo la lista dei POI mostrando nome dell'attività e città
                listTitle.add(i, venuesList.get(i).getName() + "\n" + venuesList.get(i).getCategory() + "\n" + venuesList.get(i).getCity());
            }

            // Visualizzo i risultati ottenuti nella ListView
            // Parametro 1: contesto
            // Parametro 2: layout
            // Parametro 3: View da popolare
            // Parametro 4: oggetti da visualizzare
            myAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.row_layout, R.id.list_textview, listTitle);
            mListView.setAdapter(myAdapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Toast.makeText(MainActivity.this, "You Clicked at " + venuesList.get(+position).getName(), Toast.LENGTH_SHORT).show();

                    // TODO: Fare uno startActivity() per chiamare la activity che mostrerà le info del POI cliccato (passargli le info relative a quel POI)
                    Intent intent = new Intent(MainActivity.this, PoiActivity.class);
                    intent.putExtra("json_message", result);
                    intent.putExtra("poi_name", venuesList.get(+position).getName());
                    startActivity(intent);

                }
            });

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
                            // setto il nome del plae (venue)
                            poi.setName(jsonArray.getJSONObject(i).getString("name"));

                            if (jsonArray.getJSONObject(i).has("location")) {
                                if (jsonArray.getJSONObject(i).getJSONObject("location").has("address")) {
                                    if (jsonArray.getJSONObject(i).getJSONObject("location").has("city")) {
                                        // setto la città del place (venue)
                                        poi.setCity(jsonArray.getJSONObject(i).getJSONObject("location").getString("city"));
                                    }
                                    if (jsonArray.getJSONObject(i).has("categories")) {
                                        if (jsonArray.getJSONObject(i).getJSONArray("categories").length() > 0) {
                                            if (jsonArray.getJSONObject(i).getJSONArray("categories").getJSONObject(0).has("icon")) {
                                                // setto la categoria del place (venue)
                                                poi.setCategory(jsonArray.getJSONObject(i).getJSONArray("categories").getJSONObject(0).getString("name"));
                                            }
                                        }
                                    }
                                    temp.add(poi);
                                }
                            }
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
