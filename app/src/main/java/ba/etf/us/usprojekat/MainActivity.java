package ba.etf.us.usprojekat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.text.SymbolTable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    TextView aktivnost;
    SeekBar grijac ;
    Switch sijalica ;
    TextView temperatura;
    TextView co ;
    TextView osvjetljenje;
    ProgressBar temp;
    MqttAndroidClient client;

    void setAktivnost(String h) {
        Calendar kla=Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        String strDate = dateFormat.format(kla.getTime());
        aktivnost.setText("Senzor pokreta je detektovao pokret u: "+strDate);
    }

    void setTemperaturu(String h) {
        Double ter=Double.parseDouble(h)*100;
        temperatura.setText("Temperatura iznosi: "+ter.intValue()+"C");
        temp.setProgress( ter.intValue());
    }

    void setOsvjetljenje(String h) {
        Double k=100-Double.parseDouble(h)*100;
        osvjetljenje.setText("Osvjetljenje je na: " +k.intValue()+"%");
    }
    void setCo(String h) {
        Double k=Double.parseDouble(h)*100;
        co.setText("Nivo CO2 je: "+k.intValue()+"%");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        grijac = (SeekBar) findViewById(R.id.seekBar);
        sijalica = (Switch) findViewById(R.id.on_off);
         temperatura = (TextView) findViewById(R.id.temperatura1);
         co = (TextView) findViewById(R.id.co);
         osvjetljenje = (TextView) findViewById(R.id.osvjetljenje);
        aktivnost = (TextView) findViewById(R.id.aktivnost);
        temp=(ProgressBar)findViewById(R.id.temperatura);
        temp.setMax(100);
        SharedPreferences sharedPrefs = getSharedPreferences("ba.etf.us.usprojekat", MODE_PRIVATE);
        sijalica.setChecked(sharedPrefs.getBoolean("sijalica", false));

        grijac.setProgress(sharedPrefs.getInt("grijac", 0));

        sijalica.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!b)
                {
                    try {
                        client.publish("kucanasa/sijalica", (new String("0000")).getBytes(StandardCharsets.UTF_8),0,false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                else
                    try {
                        client.publish("kucanasa/sijalica", (new String("1")).getBytes(StandardCharsets.UTF_8),0,false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
            }
        });
        grijac.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    Double d=new Double(seekBar.getProgress())/100;
                    client.publish("kucanasa/grijac", (new String(d.toString())).getBytes(StandardCharsets.UTF_8),0,false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });
        client = new MqttAndroidClient(getApplicationContext(), "tcp://broker.hivemq.com", MqttClient.generateClientId());

        MqttConnectOptions op = new MqttConnectOptions();
        op.setCleanSession(true);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i("TAG", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if(topic.contains("kucanasa/temperatura"))
                setTemperaturu(new String(message.getPayload()));
                else if(topic.contains("kucanasa/pokret")) {
                    setAktivnost(new String(message.getPayload()));
                }
                else if(topic.contains("kucanasa/osvjetljenje"))
                    setOsvjetljenje(new String(message.getPayload()));
                else if(topic.contains("kucanasa/co2"))
                    setCo(new String(message.getPayload()));

                Log.i("TAG", "topic: " + topic + ", msg: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i("TAG", "msg delivered");
            }
        });

        try {
            client.connect(op, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("TAG", "connect succeed");

                    subscribeTopic("kucanasa/co2");
                    subscribeTopic("kucanasa/pokret");
                    subscribeTopic("kucanasa/temperatura");
                    subscribeTopic("kucanasa/osvjetljenje");
                    if(!sijalica.isChecked())
                    {
                        try {
                            client.publish("kucanasa/sijalica", (new String("0000")).getBytes(StandardCharsets.UTF_8),0,false);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        try {
                            client.publish("kucanasa/sijalica", (new String("1")).getBytes(StandardCharsets.UTF_8),0,false);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    try {
                        Double d=new Double(grijac.getProgress())/100;
                        client.publish("kucanasa/grijac", (new String(d.toString())).getBytes(StandardCharsets.UTF_8),0,false);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("TAG", "connect failed");
                }
            });


        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        SharedPreferences.Editor editor = getSharedPreferences("ba.etf.us.usprojekat", MODE_PRIVATE).edit();
        editor.putInt("grijac", grijac.getProgress());
        editor.putBoolean("sijalica",sijalica.isChecked());
        editor.commit();
        super.onDestroy();
    }

    public void subscribeTopic(String topic) {
        try {
            client.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i("TAggggG", "subscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i("TAggggG", "subscribed failed");
                }
            });

        } catch (MqttException e) {

        }
    }
}