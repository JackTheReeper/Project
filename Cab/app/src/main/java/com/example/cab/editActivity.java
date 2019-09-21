package com.example.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class editActivity extends AppCompatActivity {

    private String getType;
    private String checker = "";

    private CircleImageView profileImageView;
    private EditText nameEditText, phoneEditText, driverCarName;
    private ImageView close_Button, save_Button;
    private TextView profileChangeBtn;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference DatabaseRef;
    private StorageReference mStorageRef;
    private StorageTask uploadTask;
    private Uri imguri;
    private String myurl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        getType = getIntent().getStringExtra("type");
        Toast.makeText(this, getType, Toast.LENGTH_SHORT).show();

        firebaseAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference("Profile Picture");
        DatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);

        profileImageView = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.name);
        phoneEditText = findViewById(R.id.phone_number);
        driverCarName = findViewById(R.id.driver_car_name);
        if (getType.equals("Drivers"))
        {
            driverCarName.setVisibility(View.VISIBLE);
        }
        close_Button = findViewById(R.id.closeButton);
        save_Button = findViewById(R.id.saveButton);
        profileChangeBtn = findViewById(R.id.change_picture_btn);


        close_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getType.equals("Drivers"))
                {
                    startActivity(new Intent(editActivity.this, DriverMapActivity.class));
                }
                else
                {
                    startActivity(new Intent(editActivity.this, TravelerMapActivity.class));
                }
            }
        });

        save_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checker.equals("clicked")){

                    validateController();

                } else {

                    validateAndSaveInfoOnly();

                }
            }
        });

        profileChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checker = "clicked";
                filechooser();
            }
        });

        getUserInformation();
    }

    private String getExtension(Uri uri){
        ContentResolver cr=getContentResolver();
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }

    private void fileuploader(){

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please wait, while we are settings your account information");
        progressDialog.show();

        final StorageReference ref = mStorageRef.child(firebaseAuth.getCurrentUser().getUid()+"."+getExtension(imguri));
        uploadTask = ref.putFile(imguri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Toast.makeText(editActivity.this,"Uploaded", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                        Toast.makeText(editActivity.this,"Not Uploaded", Toast.LENGTH_LONG).show();
                    }
                });

        uploadTask.continueWithTask(new Continuation() {
            @Override
            public Object then(@NonNull Task task) throws Exception
            {
                if (!task.isSuccessful())
                {
                    throw task.getException();
                }

                return ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task)
            {
                if (task.isSuccessful())
                {
                    Uri downloadUrl = task.getResult();
                    myurl = downloadUrl.toString();


                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", firebaseAuth.getCurrentUser().getUid());
                    userMap.put("name", nameEditText.getText().toString());
                    userMap.put("phone", phoneEditText.getText().toString());
                    userMap.put("image", myurl);

                    if (getType.equals("Drivers"))
                    {
                        userMap.put("car", driverCarName.getText().toString());
                    }

                    DatabaseRef.child(firebaseAuth.getCurrentUser().getUid()).updateChildren(userMap);

                    progressDialog.dismiss();

                    if (getType.equals("Drivers"))
                    {
                        startActivity(new Intent(editActivity.this, DriverMapActivity.class));
                    }
                    else
                    {
                        startActivity(new Intent(editActivity.this, TravelerMapActivity.class));
                    }
                }
            }
        });
    }

    private void filechooser(){

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/'");
        startActivityForResult(intent,1);
    }

    private void validateController(){
        if (TextUtils.isEmpty(phoneEditText.getText().toString())) {
            Toast.makeText(this,"Please enter phone", Toast.LENGTH_SHORT).show();
            phoneEditText.setError("Phone no. Required");
            phoneEditText.requestFocus();
            return;
        }

        else if (TextUtils.isEmpty(nameEditText.getText().toString()))  {
            Toast.makeText(this,"Please enter name", Toast.LENGTH_SHORT).show();
            nameEditText.setError("Name Required");
            nameEditText.requestFocus();
            return;
        }
        else if (getType.equals("Drivers") && TextUtils.isEmpty(driverCarName.getText().toString())){
            Toast.makeText(this,"Please enter name", Toast.LENGTH_SHORT).show();
            driverCarName.setError("Name Required");
            driverCarName.requestFocus();
            return;
        }
        else {
            fileuploader();
        }
    }

    private void validateAndSaveInfoOnly() {

        if (TextUtils.isEmpty(phoneEditText.getText().toString())) {
            Toast.makeText(this,"Please enter phone", Toast.LENGTH_SHORT).show();
            phoneEditText.setError("Phone no. Required");
            phoneEditText.requestFocus();
            return;
        }

        else if (TextUtils.isEmpty(nameEditText.getText().toString()))  {
            Toast.makeText(this,"Please enter name", Toast.LENGTH_SHORT).show();
            nameEditText.setError("Name Required");
            nameEditText.requestFocus();
            return;
        }
        else if (getType.equals("Drivers") && TextUtils.isEmpty(driverCarName.getText().toString())){
            Toast.makeText(this,"Please enter name", Toast.LENGTH_SHORT).show();
            driverCarName.setError("Name Required");
            driverCarName.requestFocus();
            return;
        }
        else {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid", firebaseAuth.getCurrentUser().getUid());
            userMap.put("name", nameEditText.getText().toString());
            userMap.put("phone", phoneEditText.getText().toString());

            if (getType.equals("Drivers")) {
                userMap.put("car", driverCarName.getText().toString());
            }

            DatabaseRef.child(firebaseAuth.getCurrentUser().getUid()).updateChildren(userMap);

            if (getType.equals("Drivers")) {
                startActivity(new Intent(editActivity.this, DriverMapActivity.class));
            } else {
                startActivity(new Intent(editActivity.this, TravelerMapActivity.class));
            }
        }

    }

    private void getUserInformation()
    {
        DatabaseRef.child(firebaseAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    nameEditText.setText(name);
                    phoneEditText.setText(phone);

                    if (getType.equals("Drivers")) {
                        String car = dataSnapshot.child("car").getValue().toString();
                        driverCarName.setText(car);
                    }


                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == 1 && resultCode == RESULT_OK && data.getData()!= null && data!= null){
            imguri = data.getData();
            profileImageView.setImageURI(imguri);
        }
        else {
            if (getType.equals("Drivers"))
            {
                startActivity(new Intent(editActivity.this, DriverMapActivity.class));
            }
            else
            {
                startActivity(new Intent(editActivity.this, TravelerMapActivity.class));
            }
        }

    }
}
