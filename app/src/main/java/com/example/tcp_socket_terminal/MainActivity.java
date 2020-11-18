package com.example.tcp_socket_terminal;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

import static android.widget.Toast.*;

public class MainActivity extends AppCompatActivity {

    String defaultip="10.10.10.10";
    int defaultport = 1111;

    Button btnconnect,btnsend,btnClear;
    EditText editTextIp,editTextPort,editTextMessage;
    TextView textViewMessage;

    private boolean btconnectflage=false;
    private TcpClient tcpClient = new TcpClient(defaultip,defaultport);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextIp = (EditText) findViewById(R.id.edIP);
        editTextPort=(EditText) findViewById(R.id.edPORT);
        editTextMessage=(EditText) findViewById(R.id.edSend);
        textViewMessage = (TextView) findViewById(R.id.txtMessage);

        btnClear=(Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewMessage.setText("");
            }
        });

        btnconnect = (Button) findViewById(R.id.buttonConnect);
        btnsend = (Button) findViewById(R.id.btnSend);
        //textViewMessage.setEnabled(false);
        btnconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (editTextIp.getText().toString().equals("") && editTextPort.getText().toString().equals("")){
                    Toast.makeText(MainActivity.this,"Please enter IP and PORT", Toast.LENGTH_SHORT).show();
                }
                else {
                    if(btconnectflage){
                        btconnectflage=false;
                        btnconnect.setText("Connect");
                        tcpClient.tcpunconnect();
                        textViewMessage.append("Client : Disconnected\n");
                    }
                    else {
                        btconnectflage = true;
                        btnconnect.setText("Disconnect");
                        tcpClient.setagrv(editTextIp.getText().toString(),Integer.parseInt(editTextPort.getText().toString()));
                        new Thread(tcpClient).start();
                    }
                }
            }
        });

        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!btconnectflage){
                    makeText(MainActivity.this,"please connect tcp!", LENGTH_SHORT).show();
                }
                else{
                    if(!editTextMessage.getText().toString().equals("")){
                        textViewMessage.append("Client : "+editTextMessage.getText().toString()+"\n");
                        new Thread(new Runnable(){
                            @Override
                            public void run() {
                                tcpClient.send(editTextMessage.getText().toString());

                            }
                        }).start();
                    }
                    else{
                        makeText(MainActivity.this,"please enter value", LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

     private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    String getstring = msg.obj.toString();
                    textViewMessage.append("Server : "+getstring+"\n");
                    break;
            }
        }
    };

    public class TcpClient implements Runnable{
        String serverIP=defaultip;
        int serverPort=defaultport;
        boolean isrun=false;
        private DataInputStream dataInputStream;
        private PrintWriter printWriter;
        private InputStream inputStream;
        private Socket socket = null;

        public TcpClient(String ip,int port){
            this.serverIP=ip;
            this.serverPort=port;
        }

        public void tcpunconnect(){
            isrun=false;
        }

        public void send (String msg){
            printWriter.print(msg);
            printWriter.flush();
        }

        public void setagrv(String ip,int port){
            serverIP=ip;
            serverPort=port;
        }

        @Override
        public void run() {
            int rcvLen;
            byte buff[] = new byte[4096];
            String rcvMsg;

            try{
                socket = new Socket(serverIP,serverPort);
                socket.setSoTimeout(5000);
                isrun=true;
                printWriter = new PrintWriter(socket.getOutputStream(),true);
                inputStream = socket.getInputStream();
                dataInputStream = new DataInputStream(inputStream);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewMessage.append("Client : Connected\n");
                    }
                });

            }catch (Exception e){
                btconnectflage=false;
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewMessage.append("Client : Disconnected Please enter true IP and Port");
                    }
                });

            }
            while (isrun)
            {
                try{
                    rcvLen = dataInputStream.read(buff);
                    rcvMsg = new String(buff,0 ,rcvLen,"utf-8");
                    Message message = Message.obtain();
                    message.what=1;
                    message.obj=rcvMsg;
                    handler.sendMessage(message);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            try {
                socket.close();
                printWriter.close();
                dataInputStream.close();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
    }
}