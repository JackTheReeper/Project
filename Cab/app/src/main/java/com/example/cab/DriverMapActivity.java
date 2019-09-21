package com.example.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cab.directionhelpers.FetchURL;
import com.example.cab.directionhelpers.TaskLoadedCallback;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, TaskLoadedCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastlocation;
    LocationRequest locationRequest;
    private Polyline currentPolyline;
    private MarkerOptions place1, place2;

    private Button LogoutDriverButton;
    private Button ProfileDriverButton;
    private ImageButton EditButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Boolean currentLogOutDriverStatus = false;
    private String driverID;
    private String travelerID = "";
    private DatabaseReference AssignedTravelerRef;
    private DatabaseReference AssignedTravelerPickUpRef;
    Marker PickUpMarker;
    private ValueEventListener AssignedTravelerPickUpRefListner;

    private TextView txtName, txtPhone;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;
    private ImageView phoneImage;

    private TextView txtNameDriver, txtPhoneDriver, txtCarName;
    private CircleImageView profilePicDriver;
    private TextView OccupationOfUser;
    private DrawerLayout drawerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();

        LogoutDriverButton = findViewById(R.id.driverlogOutBtn);
        ProfileDriverButton = findViewById(R.id.driverProfileBtn);

        txtName = findViewById(R.id.name_traveler);
        txtPhone = findViewById(R.id.phone_traveler);
        profilePic = findViewById(R.id.profile_image_Traveler);
        relativeLayout = findViewById(R.id.rel1);
        phoneImage = findViewById(R.id.phone_button);

        txtNameDriver = findViewById(R.id.name);
        txtPhoneDriver = findViewById(R.id.phone);
        txtCarName = findViewById(R.id.car);
        drawerLayout = findViewById(R.id.drawer_layout);

        EditButton = findViewById(R.id.edit_btn);
        EditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriverMapActivity.this, editActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ProfileDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    getDriverInformation();
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }

            }
        });

        LogoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLogOutDriverStatus = true;
                DatabaseReference DriverAvabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
                DriverAvabilityRef.setValue(null);
                onStop();
                mAuth.signOut();
                logOutDriver();
            }
        });

        phoneImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                        .child("Users").child("Travelers").child(travelerID);

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

        GetAssignedTravelerRequest();
    }

    private void GetAssignedTravelerRequest() {
        AssignedTravelerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("TravelerRideID");
        AssignedTravelerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    travelerID = dataSnapshot.getValue().toString();
                    Log.d("geoQuery", dataSnapshot.getChildren().toString());
                    GetAssignedTravelerPickUpLocation();
                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedCustomerInformation();
                } else {
                    travelerID = "";

                    if (PickUpMarker != null) {
                        PickUpMarker.remove();
                    }

                    if (AssignedTravelerPickUpRefListner != null) {
                        AssignedTravelerPickUpRef.removeEventListener(AssignedTravelerPickUpRefListner);
                    }

                    relativeLayout.setVisibility(View.INVISIBLE);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void GetAssignedTravelerPickUpLocation() {
        AssignedTravelerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Traveler Request").child(travelerID).child("l");
        AssignedTravelerPickUpRefListner = AssignedTravelerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("geoQuery", "onDataChange");
                //Toast.makeText(DriverMapActivity.this,"onDataChange", Toast.LENGTH_LONG).show();
                Log.d("geoQuery", dataSnapshot.getChildren().toString());

                methodtocall(dataSnapshot);
            }

            private void methodtocall(DataSnapshot data) {
                Log.d("geoQuery", "methodtocall");
                //Toast.makeText(DriverMapActivity.this,"methodtocall", Toast.LENGTH_LONG).show();
                Log.d("geoQuery", String.valueOf(data.exists()));
                if (data.exists()) {
                    /*List<Object> travelerLocationMap = (List<Object>) dataSnapshot.getValue();*/
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if (data.child("0").getValue() != null) {
                        LocationLat = Double.parseDouble(data.child("0").getValue().toString());
                    }
                    if (data.child("1").getValue() != null) {
                        LocationLng = Double.parseDouble(data.child("1").getValue().toString());
                    }

                        /*if (travelerLocationMap.get(0) != null) {
                            LocationLat = Double.parseDouble(travelerLocationMap.get(0).toString());
                        }
                        if (travelerLocationMap.get(1) != null) {
                            LocationLng = Double.parseDouble(travelerLocationMap.get(1).toString());
                        }*/
                    LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Pick Up Traveler From Here...").icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));

                    direction(LocationLat, LocationLng);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void direction(double locationLat, double locationLng) {

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        place1 = new MarkerOptions().position(new LatLng(latitude, longitude)).title("Location 1");
        place2 = new MarkerOptions().position(new LatLng(locationLat, locationLng)).title("Location 2");

        String url  = getUrl(place1.getPosition(), place2.getPosition(), "driving");
        new FetchURL(DriverMapActivity.this).execute(url, "driving");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
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
       if (getApplicationContext() != null){
           lastlocation = location;
           LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
           mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
           mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

           String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

           DatabaseReference DriverAvabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");

           GeoFire geoFireAvability = new GeoFire(DriverAvabilityRef);


           switch (travelerID)
           {
               case "":
                   geoFireAvability.removeLocation(userID);
                   geoFireAvability.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()),new GeoFire.CompletionListener() {
                   @Override
                   public void onComplete(String key, DatabaseError error) {
                       if (error != null) {
                           //Toast.makeText(DriverMapActivity.this, "There was an error saving the location to GeoFire: " + error, Toast.LENGTH_LONG).show();
                       } else {
                           //Toast.makeText(DriverMapActivity.this, "Location saved on server successfully!", Toast.LENGTH_LONG).show();
                       }
                   }
               });
                   break;

               default:
                   geoFireAvability.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()),new GeoFire.CompletionListener() {
                       @Override
                       public void onComplete(String key, DatabaseError error) {
                           if (error != null) {
                               //Toast.makeText(DriverMapActivity.this, "There was an error saving the location to GeoFire: " + error, Toast.LENGTH_LONG).show();
                           } else {
                              // Toast.makeText(DriverMapActivity.this, "Location saved on server successfully!", Toast.LENGTH_LONG).show();
                           }
                       }
                   });
                   break;
           }
       }
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
    protected  void onStop(){
        super.onStop();

        if(!currentLogOutDriverStatus){
            disconnectTheDriver();
        }
    }

    private void disconnectTheDriver(){
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriverAvabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        GeoFire geoFire = new GeoFire(DriverAvabilityRef);
        geoFire.removeLocation(userID);
    }

    private void logOutDriver(){
        Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void getAssignedCustomerInformation()
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

                    txtName.setText(name);
                    txtPhone.setText(phone);

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

    private void getDriverInformation()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverID);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String car = dataSnapshot.child("car").getValue().toString();

                    txtNameDriver.setText("NAME-"+name);
                    txtPhoneDriver.setText("PHONE NO.-"+phone);
                    txtCarName.setText("CAR NO.-"+car);
                    NavigationView navigationView = findViewById(R.id.navigation);
                    View v = navigationView.getHeaderView(0);
                    OccupationOfUser = v.findViewById(R.id.occupation);
                    OccupationOfUser.setText("DRIVER");
                    profilePicDriver = v.findViewById(R.id.profile_image);
                    if (dataSnapshot.hasChild("image"))
                    {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profilePicDriver);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void currentLocation(){


    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=AIzaSyBp8WRim3HSBx7ZxX68fG8ZvM8AE0ji6Ms";
        return url;
    }

    @Override
    public void onTaskDone(Object... values) {

        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);

    }
}
