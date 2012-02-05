package org.jetbrains.k2js.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.facade.K2JSTranslatorApplet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Some util class for WebDemo module.
 */
public final class ErrorSender {

    public static void sendTextToServer(@NotNull String text, @NotNull String request) {
        String urlPath = request + "/?sessionId=" + K2JSTranslatorApplet.SESSION_ID + "&writeLog=" + "error";

        URL url;
        try {
            url = new URL(urlPath);

            HttpURLConnection urlConnection;
            urlConnection = (HttpURLConnection) url.openConnection();


            urlConnection.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write("text=" + text);
            wr.flush();
            wr.close();

            urlConnection.connect();

            BufferedReader in;

            try {
                in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            } catch (Exception e) {
                in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
            }
            in.close();

        } catch (Exception e) {
            //  IGNORE
        }
    }


}
