package com.example.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class TravelerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastlocation;
    LocationRequest locationRequest;

    private Button LogoutTravelerButton;
    private Button ProfileTravelerButton;
    private ImageButton EditButton;
    private Button CallCabButton;
    private LatLng travelerPickUpLocation;
    int radius = 1;
    private Boolean currentLogOutTravelerStatus = false;
    private Boolean driverFound = false;
    private Boolean requestType = false;
    private String driverFoundID;

    private String travelerID;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    Marker DriverMarker;
    Marker PickUpMarker;
    private DatabaseReference TravelerDatabaseRef;
    private DatabaseReference DriverAvailableRef;
    private DatabaseReference DriverRef;
   //private DatabaseReference DriverLocationRef;
    GeoQuery geoQuery;
    private ValueEventListener DriverLocationRefListener;

    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;
    private ImageView phoneImage;

    private TextView txtNameTraveler, txtPhoneTraveler;
    private TextView OccupationOfUser;
    private CircleImageView profilePicTraveler;
    private DrawerLayout drawerLayout;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traveler_map);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        travelerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        TravelerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Traveler Request");
        DriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        //DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        LogoutTravelerButton = findViewById(R.id.travelerlogOutBtn);
        ProfileTravelerButton = findViewById(R.id.travelerProfileBtn);
        CallCabButton = findViewById(R.id.travelerCallCab);

        txtName = findViewById(R.id.name_driver);
        txtPhone = findViewById(R.id.phone_driver);
        txtCarName = findViewById(R.id.car_name_driver);
        profilePic = findViewById(R.id.profile_image_driver);
        relativeLayout = findViewById(R.id.rel1);

        txtNameTraveler = findViewById(R.id.name);
        txtPhoneTraveler = findViewById(R.id.phone);
        drawerLayout = findViewById(R.id.drawer_layout);
        phoneImage = findViewById(R.id.phone_button);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        EditButton = findViewById(R.id.edit_btn);
        EditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TravelerMapActivity.this,editActivity.class);
                intent.putExtra("type","Travelers");
                startActivity(intent);
            }
        });

        ProfileTravelerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    getTravelerInformation();
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }

            }
        });

        LogoutTravelerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLogOutTravelerStatus = true;
                onStop();
                mAuth.signOut();
                logOutTraveler();
            }
        });
        CallCabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(requestType){

                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriverAvailableRef.removeEventListener(DriverLocationRefListener);

                    if(driverFound != null){
                        DriverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("TravelerRideID");
                        DriverRef.setValue(null);
                        driverFoundID = null;
                    }

                    driverFound = false;
                    radius = 1;
                    String travelerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    GeoFire geoFire = new GeoFire(TravelerDatabaseRef);
                    geoFire.removeLocation(travelerID);
                    if(PickUpMarker != null){
                        PickUpMarker.remove();
                    }
                    if (DriverMarker != null){
                        DriverMarker.remove();
                    }

                    CallCabButton.setText("Call A Cab");
                    relativeLayout.setVisibility(View.INVISIBLE);

                }else{

                    requestType = true;
                    String travelerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(TravelerDatabaseRef);
                    geoFire.setLocation(travelerID, new GeoLocation(lastlocation.getLatitude(), lastlocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
                                //Toast.makeText(TravelerMapActivity.this,"There was an error saving the location to GeoFire: " + error, Toast.LENGTH_LONG).show();
                            } else {
                                //Toast.makeText(TravelerMapActivity.this,"Location saved on server successfully!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    travelerPickUpLocation = new LatLng(lastlocation.getLatitude(),lastlocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(travelerPickUpLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                    CallCabButton.setText("Getting Drivers Nearby....");
                    GetClosestDriver();

                }

            }
        });

        phoneImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                        .child("Users").child("Drivers").child(driverFoundID);

                reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                        {
                            String phone = dataSnapshot.child("phone").getValue().toString();

                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:"+phone));
                            startActivity(intent);

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });
    }

    private void GetClosestDriver() {
        GeoFire geoFire = new GeoFire(DriverAvailableRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(travelerPickUpLocation.latitude, travelerPickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (!driverFound && requestType){
                    driverFound = true;
                    driverFoundID = key;

                    DriverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("TravelerRideID",travelerID);
                    DriverRef.updateChildren(driverMap);

                    gettingDriverLocation();
                    CallCabButton.setText("Looking For Driver Location..");
                    Log.d("geoQuery","onKeyEntered " + key);
                    //Toast.makeText(TravelerMapActivity.this,"onKeyEntered " + key, Toast.LENGTH_LONG).show();


                }
            }

            @Override
            public void onKeyExited(String key) {
                //Toast.makeText(TravelerMapActivity.this,"onKeyEntered " + key, Toast.LENGTH_LONG).show();

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                //Toast.makeText(TravelerMapActivity.this,"onKeyMoved " + key, Toast.LENGTH_LONG).show();

            }

            @Override
            public void onGeoQueryReady() {
                //Toast.makeText(TravelerMapActivity.this,"onGeoQueryReady ", Toast.LENGTH_LONG).show();
                if (!driverFound){
                    radius++;
                    GetClosestDriver();
                    Log.d("geoQuery","onGeoQueryReady called");
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                //Toast.makeText(TravelerMapActivity.this,"onGeoQueryError "+ error, Toast.LENGTH_LONG).show();

            }
        });
    }

    private void gettingDriverLocation() {

            DriverLocationRefListener = DriverAvailableRef.child(driverFoundID).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("geoQuery","onDataEntered "+dataSnapshot.toString());
                //Toast.makeText(TravelerMapActivity.this,"onDataEntered "+dataSnapshot.toString(), Toast.LENGTH_LONG).show();
                Log.d("geoQuery",dataSnapshot.getChildren().toString());

                methodtocall(dataSnapshot, requestType);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void methodtocall(DataSnapshot data, Boolean request) {
        Log.d("geoQuery","methodtocall");
        //Toast.makeText(TravelerMapActivity.this,"methodtocall", Toast.LENGTH_LONG).show();
        Log.d("geoQuery", String.valueOf(data.exists()));
        if (data.exists() && request){
            /*List<?> driverLocationMap  = (List<?>) data.getValue();*/
            double LocationLat = 0;
            double LocationLng = 0;
            CallCabButton.setText("Distance Found");
            Log.d("geoQuery","Distance Found");

            relativeLayout.setVisibility(View.VISIBLE);
            getAssignedDriverInformation();
            //Toast.makeText(TravelerMapActivity.this,"Distance Found", Toast.LENGTH_LONG).show();

            if (data.child("0").getValue() != null){
                LocationLat = Double.parseDouble(data.child("0").getValue().toString());
            }
            if (data.child("1").getValue() != null){
                LocationLng = Double.parseDouble(data.child("1").getValue().toString());
            }

            /*if (driverLocationMap.get(0) != null){
                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
            }
            if (driverLocationMap.get(1) != null){
                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
            }*/

            LatLng DriverLatLng = new LatLng(LocationLat,LocationLng);

            if (DriverMarker != null){
                DriverMarker.remove();
            }

            Location location1 = new Location("");
            location1.setLatitude(travelerPickUpLocation.latitude);
            location1.setLongitude(travelerPickUpLocation.longitude);

            Location location2 = new Location("");
            location2.setLatitude(DriverLatLng.latitude);
            location2.setLongitude(DriverLatLng.longitude);

            float Distance = location1.distanceTo(location2);
            Log.d("geoQuery","Driver Found:"+Distance);
            //Toast.makeText(TravelerMapActivity.this,"Driver Found:"+Distance, Toast.LENGTH_LONG).show();

            if (Distance < 90)
            {
                CallCabButton.setText("Driver's Reached");
            }
            else
            {
                CallCabButton.setText("Driver Found: " + String.valueOf(Distance));
            }

            //CallCabButton.setText("Driver Found:");

            DriverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Your Driver Is Here...").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

        }

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //if satement vaala check krle na v09
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastlocation = location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!currentLogOutTravelerStatus){
            disconnectTheTraveler();
        }
    }

    private void disconnectTheTraveler(){
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference TravelerRef = FirebaseDatabase.getInstance().getReference().child("Traveler Requests");
        GeoFire geoFire = new GeoFire(TravelerRef);
        geoFire.removeLocation(userID);
    }

    private void logOutTraveler() {
        Intent intent = new Intent(TravelerMapActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void getAssignedDriverInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverFoundID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String car = dataSnapshot.child("car").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCarName.setText(car);

                    if (dataSnapshot.hasChild("image"))
                    {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getTravelerInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Travelers").child(travelerID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    txtNameTraveler.setText("NAME-"+name);
                    txtPhoneTraveler.setText("PHONE NO.-"+phone);
                    NavigationView navigationView = findViewById(R.id.navigation);
                    View v = navigationView.getHeaderView(0);
                    OccupationOfUser = v.findViewById(R.id.occupation);
                    OccupationOfUser.setText("TRAVELER");
                    profilePicTraveler = v.findViewById(R.id.profile_image);
                    if (dataSnapshot.hasChild("image"))
                    {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePicTraveler);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
