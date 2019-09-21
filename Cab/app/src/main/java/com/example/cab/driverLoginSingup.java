package com.example.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthActionCodeException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;


public class driverLoginSingup extends AppCompatActivity {

    Button buttonSignIn;
    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;
    private EditText editTextPassword;
    private EditText driver_Car_Name;
    private TextView DriverStatus;
    private ImageView profilepic;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference driverDatabaseRef;
    private StorageReference mStorageRef;
    private StorageTask uploadTask;
    String currentUserId;
    private Uri imguri;
    private String myurl = "";
    Button ch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_singup);

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        ch = findViewById(R.id.button);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        editTextEmail = findViewById(R.id.email);
        editTextName = findViewById(R.id.name);
        editTextPassword = findViewById(R.id.password);
        editTextPhone = findViewById(R.id.phone);
        driver_Car_Name = findViewById(R.id.carName);
        DriverStatus = findViewById(R.id.driverStatus);
        profilepic = findViewById(R.id.photo);
        mStorageRef = FirebaseStorage.getInstance().getReference("Profile Picture");

        ch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                    filechooser();
                } else {
                    filechooser();
                }
            }
        });

        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view == buttonSignIn){
                    String email = editTextEmail.getText().toString().trim();
                    String password = editTextPassword.getText().toString().trim() +"9874";
                    String phone = editTextPhone.getText().toString().trim();
                    String name = editTextName.getText().toString().trim();

                    SignInUser(email, password, phone, name);

                }
            }
        });

    }

    private String getExtension(Uri uri){
        ContentResolver cr=getContentResolver();
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));
    }

    private void fileuploader(){
         final StorageReference ref = mStorageRef.child(firebaseAuth.getCurrentUser().getUid()+"."+getExtension(imguri));
         ref.putFile(imguri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Uri downloadUrl = uri;
                                myurl = downloadUrl.toString();
                                HashMap userMap2 = new HashMap();
                                userMap2.put("image", myurl);
                                driverDatabaseRef.updateChildren(userMap2);
                                //Do what you want with the url
                                Toast.makeText(driverLoginSingup.this,"Uploaded", Toast.LENGTH_LONG).show();
                            }
                    });
                }}).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                        Toast.makeText(driverLoginSingup.this,"Not Uploaded", Toast.LENGTH_LONG).show();
                    }
                });

        /*uploadTask = ref.putFile(imguri);
        Toast.makeText(driverLoginSingup.this,"Uploaded", Toast.LENGTH_LONG).show();
        myurl =  ref.getDownloadUrl().toString();*/

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", firebaseAuth.getCurrentUser().getUid());
        userMap.put("name", editTextName.getText().toString());
        userMap.put("phone", editTextPhone.getText().toString());
        //userMap.put("image", myurl);
        userMap.put("car", driver_Car_Name.getText().toString());

        driverDatabaseRef.updateChildren(userMap);

    }

    private void filechooser(){

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/'");
        startActivityForResult(intent,1);
    }

    private void SignInUser(String email,String password,String phone,String name){


        if(email.isEmpty()){
            Toast.makeText(this,"Please enter email", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Email Required");
            editTextEmail.requestFocus();
            return;
        } else {
            editTextEmail.setError(null);
        }

        if (phone.isEmpty()) {
            Toast.makeText(this,"Please enter phone", Toast.LENGTH_SHORT).show();
            editTextPhone.setError("Phone no. Required");
            editTextPhone.requestFocus();
            return;
        } else {
            editTextPhone.setError(null);
        }

        if(name.isEmpty()){
            Toast.makeText(this,"Please enter name", Toast.LENGTH_SHORT).show();
            editTextName.setError("Name Required");
            editTextName.requestFocus();
            return;
        } else {
            editTextName.setError(null);
        }

        if(password.isEmpty()){
            Toast.makeText(this,"Please enter password", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password Required");
            editTextPassword.requestFocus();
            return;
        } else {
            if(password.length()<10){
                editTextPassword.setError("Minimum lenght greater than 6");
                editTextPassword.requestFocus();
                return;
            }
            else {
                editTextPassword.setError(null);
            }
        }

        progressDialog.setTitle("Driver");
        progressDialog.setMessage("Registering User...");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            currentUserId = firebaseAuth.getCurrentUser().getUid();
                            driverDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(currentUserId);
                            driverDatabaseRef.setValue(true);
                            fileuploader();
                            Intent intent = new Intent(driverLoginSingup.this,DriverMapActivity.class);
                            startActivity(intent);
                            Toast.makeText(driverLoginSingup.this,"Registered", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                        else {
                            if (task.getException() instanceof FirebaseAuthActionCodeException) {
                                Toast.makeText(driverLoginSingup.this, "Already Registered", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            } else {
                                Toast.makeText(driverLoginSingup.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }

                        }
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == 1 && resultCode == RESULT_OK && data.getData()!= null && data!= null){
            imguri = data.getData();
            profilepic.setImageURI(imguri);
        }

    }
}
