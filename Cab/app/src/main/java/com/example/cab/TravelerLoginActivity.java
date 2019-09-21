package com.example.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;

public class TravelerLoginActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    EditText editTextEmail;
    EditText editTextPassword;
    ProgressBar progressBar;
    private TextView textViewSignUp;
    Button ch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traveler_login);

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressbar);
        textViewSignUp = findViewById(R.id.textviewSignUp);
        ch = findViewById(R.id.buttonLogIn);
        mAuth = FirebaseAuth.getInstance();
        ch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim() +"1234";
                userLogin(email,password);
            }
        });

        textViewSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view == textViewSignUp){
                    //login user
                    Intent i = new Intent(getApplicationContext(),travelersLoginSignip.class);
                    startActivity(i);
                }
            }
        });
    }

    private void userLogin(String email,String password){


        if(email.isEmpty()){
            Toast.makeText(this,"Email is required", Toast.LENGTH_SHORT).show();
            editTextEmail.setError("Email Required");
            editTextEmail.requestFocus();
            return;
        } else {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    editTextEmail.setError("Please enter valid Email");
                    editTextEmail.requestFocus();
                    return;}
                else {
                    editTextEmail.setError(null); }
                }


        if(password.isEmpty()){
            Toast.makeText(this,"Please enter password", Toast.LENGTH_SHORT).show();
            editTextPassword.setError("Password Required");
            editTextPassword.requestFocus();
            return;
        } else {
            if(password.length()<6){
                editTextPassword.setError("Minimum lenght greater than 6");
                editTextPassword.requestFocus();
                return;
            }
            else {
                editTextPassword.setError(null);
            }
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(TravelerLoginActivity.this ,new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Intent intent = new Intent(TravelerLoginActivity.this,TravelerMapActivity.class);
                    startActivity(intent);
                    Toast.makeText(TravelerLoginActivity.this,"Traveler Login Successfully..", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                } else {
                    Toast.makeText(TravelerLoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}
