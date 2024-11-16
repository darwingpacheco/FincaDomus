package com.example.fincadomus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ArrayAdapter<String> deviceListAdapter;
    private ListView lvDevices;
    private ArrayList<BluetoothDevice> devices;
    private InputStream inputStream;
    private TextView tv_temperature, tv_humedad;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Switch switchBluetooth, switchLedsCasa, switchLedsGranero, switchLedsPiscina, switchPump, switchGraneroDoor;
    private Button btnRegisterFingerprint, btnCloseFingerprintDoor;
    private final UUID MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        lvDevices = findViewById(R.id.lv_devices);
        devices = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvDevices.setAdapter(deviceListAdapter);

        // Referencias a los nuevos componentes del XML
        switchBluetooth = findViewById(R.id.switch_bluetooth);
        switchLedsCasa = findViewById(R.id.switch_leds_casa);
        switchLedsGranero = findViewById(R.id.switch_leds_granero);
        switchLedsPiscina = findViewById(R.id.switch_leds_piscina);
        switchPump = findViewById(R.id.switch_pump);
        switchGraneroDoor = findViewById(R.id.switch_granero_door);
        btnRegisterFingerprint = findViewById(R.id.btn_register_fingerprint);
        btnCloseFingerprintDoor = findViewById(R.id.btn_close_fingerprint_door);
        tv_temperature = findViewById(R.id.tv_temperature);
        tv_humedad = findViewById(R.id.tv_humedad);

        startListeningForData();
        setupBluetoothSwitch();
        setupDeviceList();
        setupCommandSwitchesAndButtons();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        startListeningForData();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        if (bluetoothAdapter.isEnabled())
            switchBluetooth.setChecked(true);
        else
            bluetoothAdapter.disable();
        listDevices();
    }

    private void setupBluetoothSwitch() {
        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                enableBluetooth();
            else
                disableBluetooth();
        });
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        checkBluetoothPermissions();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            Toast.makeText(this, "Bluetooth ya está encendido", Toast.LENGTH_SHORT).show();
            listDevices();
        }
    }

    @SuppressLint("MissingPermission")
    private void disableBluetooth() {
        checkBluetoothPermissions();
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            Toast.makeText(this, "Bluetooth apagado", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            listDevices();
        }
    }

    @SuppressLint("MissingPermission")
    private void listDevices() {
        try {
            deviceListAdapter.clear();
            devices.clear();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                devices.add(device);
            }
            deviceListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al listar dispositivos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        checkBluetoothPermissions();
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MODULE_UUID);
            bluetoothSocket.connect();
            Toast.makeText(this, "Conectado a " + device.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al conectar", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDeviceList() {
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = devices.get(position);
            connectToDevice(device);
        });
    }

    private void setupCommandSwitchesAndButtons() {
        switchLedsCasa.setOnCheckedChangeListener((buttonView, isChecked) -> sendCommand(isChecked ? "LED_CASA_ON" : "LED_CASA_OFF"));
        switchLedsGranero.setOnCheckedChangeListener((buttonView, isChecked) -> sendCommand(isChecked ? "LED_GRANERO_ON" : "LED_GRANERO_OFF"));
        switchLedsPiscina.setOnCheckedChangeListener((buttonView, isChecked) -> sendCommand(isChecked ? "LED_PISCINA_ON" : "LED_PISCINA_OFF"));
        switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> sendCommand(isChecked ? "PUMP_ON" : "PUMP_OFF"));
        switchGraneroDoor.setOnCheckedChangeListener((buttonView, isChecked) -> sendCommand(isChecked ? "DOOR_GRANERO_OPEN" : "DOOR_GRANERO_CLOSE"));

        btnRegisterFingerprint.setOnClickListener(v -> sendCommand("REGISTER_FINGERPRINT"));
        btnCloseFingerprintDoor.setOnClickListener(v -> sendCommand("CLOSE_FINGERPRINT_DOOR"));
    }

    private void sendCommand(String command) {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(command.getBytes());
                Toast.makeText(this, "Comando enviado: " + command, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No hay conexión Bluetooth", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al enviar comando", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos Bluetooth otorgados", Toast.LENGTH_SHORT).show();
                listDevices();
            }
        }
    }

    private void startListeningForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        String receivedData = new String(buffer, 0, bytes);

                        if (receivedData.contains("temperatura"))
                            handler.post(() -> tv_temperature.setText(receivedData.replace("temperatura", "")));
                        else if (receivedData.contains("humedad"))
                            handler.post(() -> tv_humedad.setText(receivedData.replace("humedad", "")));

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }
}