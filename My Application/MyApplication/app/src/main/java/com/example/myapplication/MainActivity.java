package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    LocationListener locationListener;
    Handler handler;
    boolean isSending = true; // Envío automático
    private TextView textViewLocation; // TextView para mostrar ubicación

    // IP y puertos
    private static final String[] IP_ADDRESSES = {"3.15.13.48"};
    private static final int[] PORTS = {6001};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewLocation = findViewById(R.id.textViewLocation); // Inicializar el TextView

        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;  // Detener si los permisos no están concedidos
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        handler = new Handler(Looper.getMainLooper());

        // Verificar si el GPS está activado
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Por favor, activa el GPS", Toast.LENGTH_LONG).show();
        }

        // Configurar el LocationListener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (isSending) {
                    String lat = String.valueOf(location.getLatitude());
                    String lon = String.valueOf(location.getLongitude());
                    String timestamp = String.valueOf(location.getTime());

                    // Crear el mensaje en formato CSV
                    String message = lat + "," + lon + "," + timestamp;

                    // Actualizar el TextView con la ubicación
                    runOnUiThread(() -> textViewLocation.setText("Latitud: " + lat + "\nLongitud: " + lon + "\nTimestamp: " + timestamp));

                    // Enviar a todas las direcciones IP y puertos configurados
                    for (int i = 0; i < IP_ADDRESSES.length; i++) {
                        sendMessageOverUDP(IP_ADDRESSES[i], PORTS[i], message);
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Iniciar la solicitud de ubicación inmediatamente
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);

        // Iniciar el envío automático
        handler.post(updateLocationRunnable);
    }

    private void stopSending() {
        if (isSending) {
            isSending = false;
            handler.removeCallbacks(updateLocationRunnable);
            Toast.makeText(MainActivity.this, "Envío de paquetes detenido", Toast.LENGTH_SHORT).show();
        }
    }

    private final Runnable updateLocationRunnable = new Runnable() {
        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
            }
            handler.postDelayed(this, 10000); // Repetir cada n milisegundos
        }
    };

    // Método para enviar mensaje usando UDP
    private void sendMessageOverUDP(String ip, int port, String message) {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress ipAddress = InetAddress.getByName(ip);
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), ipAddress, port);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al enviar el mensaje por UDP", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}


