package com.invcolors;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogActivity extends Activity {

    private TextView logTextView;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("InvColors Debug Logs");
        titleView.setTextSize(20);
        titleView.setPadding(0, 0, 0, 20);
        mainLayout.addView(titleView);

        // Scroll view for logs
        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        );
        scrollView.setLayoutParams(scrollParams);

        // Log text view
        logTextView = new TextView(this);
        logTextView.setTextSize(12);
        logTextView.setPadding(10, 10, 10, 10);
        logTextView.setBackgroundColor(0xFF1E1E1E);
        logTextView.setTextColor(0xFF00FF00);
        scrollView.addView(logTextView);
        mainLayout.addView(scrollView);

        // Buttons layout
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 20, 0, 0);

        // Refresh button
        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh Logs");
        refreshButton.setOnClickListener(v -> loadLogs());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        buttonParams.setMargins(0, 0, 10, 0);
        refreshButton.setLayoutParams(buttonParams);
        buttonLayout.addView(refreshButton);

        // Copy button
        Button copyButton = new Button(this);
        copyButton.setText("Copy Logs");
        copyButton.setOnClickListener(v -> copyLogs());
        copyButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        buttonLayout.addView(copyButton);

        mainLayout.addView(buttonLayout);
        setContentView(mainLayout);

        // Load logs initially
        loadLogs();
    }

    private void loadLogs() {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("logcat -d -s InvColors:* *:S");
                BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

                StringBuilder log = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.append(line).append("\n");
                }

                String logText = log.toString();
                if (logText.isEmpty()) {
                    logText = "No InvColors logs found.\n\n" +
                             "Possible reasons:\n" +
                             "1. Module not enabled in LSPosed\n" +
                             "2. No apps selected in module scope\n" +
                             "3. Selected apps haven't been launched yet\n\n" +
                             "Try:\n" +
                             "- Enable module in LSPosed Manager\n" +
                             "- Add target apps to scope\n" +
                             "- Force stop and relaunch target apps\n" +
                             "- Then refresh logs here";
                }

                final String finalLog = logText;
                runOnUiThread(() -> {
                    logTextView.setText(finalLog);
                    scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    logTextView.setText("Error reading logs: " + e.getMessage());
                });
            }
        }).start();
    }

    private void copyLogs() {
        String logs = logTextView.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("InvColors Logs", logs);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show();
    }
}
