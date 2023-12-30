package com.iot.smart_lighting;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.iot.smart_lighting.Model.SmartLampDB;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class VoiceRecognition implements RecognitionListener {

    private Context context;
    private SpeechRecognizer recognizer;
    private Esp32 esp32;
    // Named searches allow to quickly reconfigure the decoder
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";
    private static final String COMMAND_SEARCH = "commands";

    // Keyword we are looking for to activate menu
    private static final String KEYPHRASE = "hey phoenix";

    // Initialize Database
    SmartLampDB myDB;
    SQLiteDatabase sqlDB;

    // Global Variable to store the lamp use for stop timer and colour
    private String lamp;

    public VoiceRecognition(Context context) {
        this.context = context;
        this.esp32 = new Esp32(context);
        setupRecognizer();
        this.myDB = new SmartLampDB(context);
    }

    private void setupRecognizer() {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them
        try {
            Assets assets = new Assets(context);
            File assetsDir = assets.syncAssets();

            recognizer = SpeechRecognizerSetup.defaultSetup()
                    .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                    .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                    .setKeywordThreshold(1e-45f) // Threshold to tune for keyphrase to balance between false positives and false negatives
                    .getRecognizer();

            recognizer.addListener(this);

            // Create keyword-activation search.
            recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

            // Create grammar-based search for selection between demos
            File menuGrammar = new File(assetsDir, "menu.gram");
            recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

            // Create grammar-based search for digit recognition
            File digitsGrammar = new File(assetsDir, "digits.gram");
            recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

            // Create language model search
            File languageModel = new File(assetsDir, "weather.dmp");
            recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

            // Phonetic search
            File phoneticModel = new File(assetsDir, "en-phone.dmp");
            recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);

            // Command Search
            File commandsGrammar = new File(assetsDir, "command.gram");
            recognizer.addGrammarSearch(COMMAND_SEARCH, commandsGrammar);
            switchSearch(KWS_SEARCH);
        } catch (IOException exception) {
            exception.printStackTrace();
            Toast.makeText(context, "Failed to set up voice recognition.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        // Do Something
    }

    // Stop recognizer to get the final result
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }

        String text = hypothesis.getHypstr();
        switch (text) {
            case KEYPHRASE:
                switchSearch(MENU_SEARCH);
                break;
            case DIGITS_SEARCH:
                switchSearch(DIGITS_SEARCH);
                break;
            case PHONE_SEARCH:
                switchSearch(PHONE_SEARCH);
                break;
            case FORECAST_SEARCH:
                switchSearch(FORECAST_SEARCH);
                break;
            case COMMAND_SEARCH:
                switchSearch(COMMAND_SEARCH);
            default:
                Log.d("Voice Recognition", "Listening: " + text);
                break;
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            Log.d("Voice Result", "Result: " + text);
            if (text.equals("hey phoenix") || text.equals("commands")) {
                Toast.makeText(context, "I'm Listening...", Toast.LENGTH_SHORT).show();
            }
            executeCommand(text);
        }
    }

    @Override
    public void onError(Exception e) {
        // Do Something
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH)) {
            recognizer.startListening(searchName);
        } else {
            recognizer.startListening(searchName, 10000);
        }
    }

    // Function for voice recognition to do what user command
    private void executeCommand(String textResult) {
        // Voice Command for Timer
        // Use pattern and matcher to detect word spoken match for the timer
        Pattern timerPattern = Pattern.compile("set timer to (.+) for (lamp \\w+)");
        Matcher timerMatcher = timerPattern.matcher(textResult);
        if (timerMatcher.matches()) {
            String durationString = timerMatcher.group(1);
            String lampString = timerMatcher.group(2);
            // Convert the text duration to an integer value
            int duration = convertDurationStringToEndpoint(durationString);
            lamp = convertLampStringToEndPoint(lampString);
            String endPoint = "http://192.168.4.1/" + lamp + "?timer=" + duration;
            esp32.applyLamp(endPoint);
            updateLamp(Integer.parseInt(lamp.replaceAll("[\\D]", "")), 0, 0);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        }

        // Voice Command for Colour
        // Use pattern and matcher to detect word spoken match for the timer
        Pattern colourPattern = Pattern.compile("set (.+) colour for (lamp \\w+)");
        Matcher colourMatcher = colourPattern.matcher(textResult);
        if (colourMatcher.matches()) {
            String colourString = colourMatcher.group(1);
            String lampString = colourMatcher.group(2);
            lamp = convertLampStringToEndPoint(lampString);
            int[] rgb = convertColourToRGB(colourString);
            if (rgb != null && rgb.length == 3) {
                int red = rgb[0];
                int green = rgb[1];
                int blue = rgb[2];
                // Convert RGB to hex color
                String hexColour = String.format("#%02X%02X%02X", red, green, blue);
                String endPoint = "http://192.168.4.1/" + lamp + "/colour?red=" + red + "&green=" + green + "&blue=" + blue;
                esp32.applyLamp(endPoint);
                updateColour(Integer.parseInt(lamp.replaceAll("[\\D]", "")), hexColour);
                Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
            }
        }

        if (textResult.equals("turn on lamp one")) {
            esp32.applyLamp("http://192.168.4.1/lamp1/on?value=255");
            updateLamp(1, 1, 255);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("turn off lamp one")) {
            esp32.applyLamp("http://192.168.4.1/lamp1/off");
            updateLamp(1, 0, 0);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("turn on lamp two")) {
            esp32.applyLamp("http://192.168.4.1/lamp2/on?value=255");
            updateLamp(2, 1, 255);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("turn off lamp two")) {
            esp32.applyLamp("http://192.168.4.1/lamp2/off");
            updateLamp(2, 0, 0);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("turn on lamp three")) {
            esp32.applyLamp("http://192.168.4.1/lamp3/on?value=255");
            updateLamp(3, 1, 255);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("turn off lamp three")) {
            esp32.applyLamp("http://192.168.4.1/lamp3/off");
            updateLamp(3, 0, 0);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("turn on all lamps")) {
            esp32.applyLamp("http://192.168.4.1/lamp1/on?value=255");
            esp32.applyLamp("http://192.168.4.1/lamp2/on?value=255");
            esp32.applyLamp("http://192.168.4.1/lamp3/on?value=255");
            updateLamp(1, 1, 255);
            updateLamp(2, 1, 255);
            updateLamp(3, 1, 255);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("turn off all lamps")) {
            esp32.applyLamp("http://192.168.4.1/lamp1/off");
            esp32.applyLamp("http://192.168.4.1/lamp2/off");
            esp32.applyLamp("http://192.168.4.1/lamp3/off");
            updateLamp(1, 0, 0);
            updateLamp(2, 0, 0);
            updateLamp(3, 0, 0);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("stop timer")) {
            esp32.applyLamp("http://192.168.4.1/timer?stop");
            if (lamp != null) {
                revertLampState();
            }
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (textResult.equals("open data analysis")) {
            Intent intent = new Intent(context, DataAnalysis.class);
            context.startActivity(intent);
            Toast.makeText(context, "Okay!", Toast.LENGTH_SHORT).show();
        } else if (!textResult.equals("hey phoenix") && !textResult.equals("commands") &&
                !timerMatcher.matches() && !colourMatcher.matches()) {
            Toast.makeText(context, "Sorry! I didn't get that.", Toast.LENGTH_SHORT).show();
        }
    }

    // Function to update initial state of each lamp
    private void updateLamp(int lampId, int newStatus, int intensity) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            // Open The Database for Write
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            cv.put("intensity", intensity);
            sqlDB.update("lamp", cv, "id = ?", new String[]{String.valueOf(lampId)});
        } finally {
            sqlDB.close();
        }
    }

    // Function to update colour in SQLite DB
    private void updateColour(int lampId, String colour) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            // Open The Database for Write
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("colour", colour);
            sqlDB.update("lampColour", cv, "id = ?", new String[] {String.valueOf(lampId)});
        } finally {
            sqlDB.close();
        }
    }

    // Function to convert voice text to string value for endpoint to send request to ESP32
    private String convertLampStringToEndPoint(String lampString) {
        switch (lampString) {
            case "lamp one":
                return "lamp1";
            case "lamp two":
                return "lamp2";
            case "lamp three":
                return "lamp3";
            default:
                return "unknown";
        }
    }

    // Function to update the lamp back to on in SQLite database
    private void revertLampState() {
        switch (lamp) {
            case "lamp1":
                updateLamp(1, 1, 255);
                break;
            case "lamp2":
                updateLamp(2, 1, 255);
                break;
            case "lamp3":
                updateLamp(3, 1, 255);
                break;
            default:
                break;
        }
    }

    // Function to convert colour detect to RGB value
    private int[] convertColourToRGB(String colourDetect) {
        int[] rgb;
        switch (colourDetect) {
            case "red":
                rgb = new int[] {255, 0, 0};
                break;
            case "yellow":
                rgb = new int[] {255, 255, 153};
                break;
            case "blue":
                rgb = new int[] {0, 0, 255};
                break;
            case "orange":
                rgb = new int[] {255, 165, 0};
                break;
            case "green":
                rgb = new int[] {0, 255, 0};
                break;
            case "violet":
                rgb = new int[] {138, 43, 226};
                break;
            case "indigo":
                rgb = new int[] {75, 0, 130};
                break;
            case "pink":
                rgb = new int[] {255, 192, 203};
                break;
            case "lavender":
                rgb = new int[] {230, 230, 250};
                break;
            case "cyan":
                rgb = new int[] {0, 255, 255};
                break;
            case "crimson":
                rgb = new int[] {220, 20, 60};
                break;
            case "maroon":
                rgb = new int[] {128, 0, 0};
                break;
            case "teal":
                rgb = new int[] {0, 128, 128};
                break;
            case "turquoise":
                rgb = new int[] {48, 213, 200};
                break;
            default:
                rgb = new int[] {255, 255, 255};
        }
        return rgb;
    }

    // Function to convert voice text to integer value for endpoint to send the request to ESP32
    private int convertDurationStringToEndpoint(String durationString) {
        // Map to store the mapping between textual representations and values
        Map<String, Integer> map = new HashMap<>();
        map.put("one seconds", 1);
        map.put("two seconds", 2);
        map.put("three seconds", 3);
        map.put("four seconds", 4);
        map.put("five seconds", 5);
        map.put("six seconds", 6);
        map.put("seven seconds", 7);
        map.put("eight seconds", 8);
        map.put("nine seconds", 9);
        map.put("ten seconds", 10);
        map.put("eleven seconds", 11);
        map.put("twelve seconds", 12);
        map.put("thirteen seconds", 13);
        map.put("fourteen seconds", 14);
        map.put("fifteen seconds", 15);
        map.put("sixteen seconds", 16);
        map.put("seventeen seconds", 17);
        map.put("eighteen seconds", 18);
        map.put("nineteen seconds", 19);
        map.put("twenty seconds", 20);
        map.put("twenty one seconds", 21);
        map.put("twenty two seconds", 22);
        map.put("twenty three seconds", 23);
        map.put("twenty four seconds", 24);
        map.put("twenty five seconds", 25);
        map.put("twenty six seconds", 26);
        map.put("twenty seven seconds", 27);
        map.put("twenty eight seconds", 28);
        map.put("twenty nine seconds", 29);
        map.put("thirty seconds", 30);
        map.put("thirty one seconds", 31);
        map.put("thirty two seconds", 32);
        map.put("thirty three seconds", 33);
        map.put("thirty four seconds", 34);
        map.put("thirty five seconds", 35);
        map.put("thirty six seconds", 36);
        map.put("thirty seven seconds", 37);
        map.put("thirty eight seconds", 38);
        map.put("thirty nine seconds", 39);
        map.put("forty seconds", 40);
        map.put("forty one seconds", 41);
        map.put("forty two seconds", 42);
        map.put("forty three seconds", 43);
        map.put("forty four seconds", 44);
        map.put("forty five seconds", 45);
        map.put("forty six seconds", 46);
        map.put("forty seven seconds", 47);
        map.put("forty eight seconds", 48);
        map.put("forty nine seconds", 49);
        map.put("fifty seconds", 50);
        map.put("fifty one seconds", 51);
        map.put("fifty two seconds", 52);
        map.put("fifty three seconds", 53);
        map.put("fifty four seconds", 54);
        map.put("fifty five seconds", 55);
        map.put("fifty six seconds", 56);
        map.put("fifty seven seconds", 57);
        map.put("fifty eight seconds", 58);
        map.put("fifty nine seconds", 59);
        map.put("sixty seconds", 60);

        return map.getOrDefault(durationString, 0);
    }
}
