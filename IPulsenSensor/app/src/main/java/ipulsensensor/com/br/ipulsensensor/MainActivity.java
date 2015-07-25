package ipulsensensor.com.br.ipulsensensor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    private Button btnConexao, btnSensor;
    private TextView lblBatimento;
    private EditText edtMultiLine;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean ativadoComunicacao = false, ativadoSensor = false;

    private BluetoothAdapter meuBluetooth = null;
    private BluetoothDevice mmDevice = null;
    private BluetoothSocket mmSocket;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;

    String batimentos = "";

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean pararInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConexao = (Button) findViewById(R.id.btnAtivarComunicacao);
        btnSensor = (Button) findViewById(R.id.btnLigarSensor);
        edtMultiLine = (EditText) findViewById(R.id.editMultiLine);
        edtMultiLine.setEnabled(false);

        lblBatimento = (TextView) findViewById(R.id.lblBpm);

        meuBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (meuBluetooth.isEnabled()){
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i,REQUEST_ENABLE_BT);
        }

        btnConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conectar();
            }
        });

        btnSensor.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(!ativadoComunicacao) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("Comunicação");
                    dialog.setMessage("A comunicação com o modulo bluetooth está desativada!");
                    dialog.setIcon(android.R.drawable.alert_light_frame);
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    dialog.show();
                } else {
                    try {
                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected void onPreExecute() {
                                //MainActivity.this.preexecute();
                                super.onPreExecute();
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                //MainActivity.this.postexecute();
                                super.onPostExecute(aVoid);
                            }

                            @Override
                            protected Void doInBackground(Void... params) {
                                //Looper.prepare();
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainActivity.this.sensor();
                                    }
                                });
                                return null;
                            }
                        }.execute();
                    } catch (Exception e){
                        pararInput = true;
                        btnSensor.setText("Ligar Sensor");
                        btnSensor.setBackgroundColor(Color.parseColor("#ffff0705"));
                        ativadoSensor = false;
                    }
                }
            }
        });
    }

    public void conectar(){
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        if (mmDevice == null) {
            Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    mmDevice = device;
                }
            }
        }
        try {
            if (mmSocket == null) {
                ProgressDialog ringProgressDialog = ProgressDialog.show(MainActivity.this, "Aguarde ...",
                        "Conectando ao módulo bluetooth ...", true);
                //ringProgressDialog.setCancelable(true);
                ringProgressDialog.show();

                mmSocket = mmDevice.createRfcommSocketToServiceRecord(MEU_UUID);
                mmSocket.connect();

                ringProgressDialog.dismiss();
                conectarSocket();
            } else {
                conectarSocket();
            }
        } catch (Exception e ){
            Log.e("Teste", "Erro: " + e.getMessage().toString());
        }
    }

    public void conectarSocket() throws IOException {
        if (!ativadoComunicacao) {
            Log.e("Teste", "Socket Conectado");
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            String mensagem = "1";
            byte[] msgBuffer = mensagem.getBytes();

            mmOutputStream.write(msgBuffer);

            btnConexao.setText("Desativar Comunicação");
            btnConexao.setBackgroundColor(Color.parseColor("#FF1DFF0D"));
            Log.e("Teste", "Ativou Conexao");
            ativadoComunicacao = true;
        } else {
            String mensagem = "0";
            byte[] msgBuffer = mensagem.getBytes();

            mmOutputStream.write(msgBuffer);

            btnConexao.setText("Ligar Comunicação");
            btnConexao.setBackgroundColor(Color.parseColor("#ffff0705"));
            Log.e("Teste", "Desativou Conexao");
            ativadoComunicacao = false;
            pararInput = true;
            btnSensor.setText("Ligar Sensor");
            btnSensor.setBackgroundColor(Color.parseColor("#ffff0705"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sensor(){
        if (!ativadoSensor) {
            beginListenForData();
            btnSensor.setText("Desligar Sensor");
            btnSensor.setBackgroundColor(Color.parseColor("#FF1DFF0D"));
            ativadoSensor = true;
        } else {
            pararInput = true;
            btnSensor.setText("Ligar Sensor");
            btnSensor.setBackgroundColor(Color.parseColor("#ffff0705"));
            ativadoSensor = false;
        }
    }

    void beginListenForData() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final byte delimiter = 10; //This is the ASCII code for a newline character

        pararInput = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !pararInput) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            trataRetorno(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        pararInput = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void trataRetorno(String s) {
        Log.e("Teste", "Retorno: " + s);
        batimentos += s + "\n";
        edtMultiLine.setText(batimentos);
        //edtMultiLin
        lblBatimento.setText(s + " BPM");
    }
}
