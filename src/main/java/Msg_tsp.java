import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.utils.URIBuilder;


import org.json.JSONObject;



public class Msg_tsp {

    private String filePath;
    private String DISTANCE_FILE ="distances.csv";
    private String OSRM_LINK = "http://localhost:5000/";

    Msg_tsp(String filePath) {
        this.filePath = filePath;
    }

    void solveTsp(boolean recalculateDistances) {
        if(recalculateDistances) {
            this.recalculateDistances();
        }
        // Solve TSP
        // Service nutzt Längengrad;Breitengrad als Parameter
        // "der dich auf dem kürzesten Weg zu jedem Standort der msg führt"
    }

    void recalculateDistances() {
        try{
            List<MsgHeadquarter> hqs = loadHQ();
            double[][] distanceMatrix = new double[hqs.size()][hqs.size()];
            for(int i = 0; i < hqs.size(); i++) {
                for(int j = 0; j <=i; j++) {
                    // The distance between the same points is 0.0
                    if(i != j) {
                        MsgHeadquarter hqOne = hqs.get(i);
                        MsgHeadquarter hqTwo = hqs.get(j);
                        // Service is using Longitude, latitude to describe a coordinate
                        String coordinates = hqOne.getLongitude() + "," +hqOne.getLatitude() + ";" + hqTwo.getLongitude() + "," + hqTwo.getLatitude();
                        String url = this.OSRM_LINK + "route/v1/driving/" + coordinates;

                        HttpClient httpClient = HttpClients.createDefault();

                        URIBuilder builder = new URIBuilder(url);
                        URI uri = builder.build();
                        HttpGet request = new HttpGet(uri);
                        request.addHeader("accept", "application/json");
                        HttpResponse response = httpClient.execute(request);
                        JSONObject result = new JSONObject(new String(response.getEntity().getContent().readAllBytes()));
                        double dist = result.getJSONArray("routes").getJSONObject(0).getDouble("distance");
                        distanceMatrix[i][j] = dist;
                        distanceMatrix[j][i] = dist;
                    } else {
                        distanceMatrix[i][j] = 0.0;
                    }
                }
            }
            System.out.println(Arrays.deepToString(distanceMatrix));
        } catch (IOException iox) {
            System.err.println("IOException occured");
            System.err.println(Arrays.toString(iox.getStackTrace()));
        } catch(URISyntaxException uriSyntaxException) {
            System.err.println("URISyntaxException occured");
            System.err.println(Arrays.toString(uriSyntaxException.getStackTrace()));
        }
    }

    List<MsgHeadquarter> loadHQ() throws IOException{
        ColumnPositionMappingStrategy<MsgHeadquarter> ms = new ColumnPositionMappingStrategy<>();
        ms.setType(MsgHeadquarter.class);
        BufferedReader reader = new BufferedReader(new FileReader(this.filePath));
        CsvToBean<MsgHeadquarter> cb = new CsvToBeanBuilder<MsgHeadquarter>(reader)
                // Skip Header
                .withSkipLines(1)
                .withMappingStrategy(ms)
                .build();

        List<MsgHeadquarter> list = cb.parse();
        return Objects.requireNonNullElseGet(list, ArrayList::new);
    }
}
