package app.example.com.app_sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.app.KtUsageKt;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KtUsageKt.f();
    }
}
