package com.google.ar.sceneform.samples.augmentedimages;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainPage extends AppCompatActivity {

    public Button scanObjectBTN, scanQRBTN, scanOriBTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        scanObjectBTN = (Button)findViewById(R.id.BTNscanObject);
        scanQRBTN = (Button)findViewById(R.id.BTNscanCode);
        scanOriBTN = (Button)findViewById(R.id.BTNscanORI);

        scanObjectBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainPage.this, MainActivityObject.class));
            }
        });
        scanQRBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainPage.this, MainActivityQR.class));
            }
        });
        scanOriBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainPage.this, MainActivityOri.class));
            }
        });

    }
}