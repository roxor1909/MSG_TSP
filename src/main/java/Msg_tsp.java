import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import com.opencsv.exceptions.CsvException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.utils.URIBuilder;

import com.google.common.primitives.Booleans;

import org.json.JSONObject;



public class Msg_tsp {

    private final String FILE_PATH;
    private final String DISTANCE_FILE ="src/main/resources/distances.csv";
    private final String OSRM_LINK = "http://localhost:5000/";

    Msg_tsp(String filePath) {
        this.FILE_PATH = filePath;
    }

    void solveTsp(boolean recalculateDistances) {
        if(recalculateDistances) {
            this.recalculateDistances();
        }
        // Solve TSP
        // Service nutzt Längengrad;Breitengrad als Parameter
        // "der dich auf dem kürzesten Weg zu jedem Standort der msg führt"

        double[][] distances = this.loadDistances();
        // Ismaning is the first row in the file
        int indexOfStartHq = 0;
        nearestNeighbor(distances, indexOfStartHq);

    }

    void nearestNeighbor(double[][] distances, int startIndex) {
        // Init the route
        boolean[] cityVisited = new boolean[distances.length];
        List<Integer> route = new LinkedList<>();
        route.add(startIndex);
        cityVisited[startIndex] = true;
        int currentCity = startIndex;
        System.out.println(Arrays.toString(cityVisited));
        while(Booleans.asList(cityVisited).contains(false)) {
            currentCity = this.findNextUnvisitedCity(distances, currentCity, cityVisited);
            route.add(currentCity);
            cityVisited[currentCity] = true;
        }
        // Go back to the start city
        route.add(startIndex);
        System.out.println(this.describeRoute(route));
        System.out.println(this.calculateDistanceOfRoute(distances, route) + " meters");
    }


    int findNextUnvisitedCity(double[][] distances, int currentCity, boolean[] visitedCities) {
        double smallestDistance = Double.MAX_VALUE;
        // Returns the same city if there is no unvisited neighbour
        int indexOfSmallestDistance = currentCity;
        double[] neighbours = distances[currentCity];
        int index = 0;
        for(double neighbour: neighbours) {
            if(index != currentCity && !visitedCities[index]){
                if(neighbour < smallestDistance) {
                    smallestDistance = neighbour;
                    indexOfSmallestDistance = index;
                }
            }
            index++;
        }
        return  indexOfSmallestDistance;
    }

    private double calculateDistanceOfRoute(double[][] distances, List<Integer> route) {
        double distance = 0.0;
        for(int index = 1; index < route.size(); index++) {
            distance += distances[route.get(index-1)][route.get(index)];
        }
        return  distance;
    }

    private String describeRoute(List<Integer> route) {
        StringBuilder cities = new StringBuilder();
        try{
            List<MsgHeadquarter> hqs = this.loadHQ();
            for(int cityIndex : route) {
                cities.append(hqs.get(cityIndex).getName());
                cities.append(" -> ");
            }

        }catch(IOException ioException) {
            System.err.println(Arrays.toString(ioException.getStackTrace()));
        }
        return cities.toString();
    }

    void recalculateDistances() {
        try{
            List<MsgHeadquarter> hqs = loadHQ();
            String[][] distanceMatrix = new String[hqs.size()][hqs.size()];
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
                        String dist = " " + result.getJSONArray("routes").getJSONObject(0).getDouble("distance");
                        // Assume that on the way from msg hq i to hq j is no one way street. It is a symmetric TSP. Therefore, use the same distance for the way back.
                        distanceMatrix[i][j] = dist;
                        distanceMatrix[j][i] = dist;
                    } else {
                        distanceMatrix[i][j] = "0.0";
                    }
                }
            }
            saveDistanceMatrix(distanceMatrix);
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
        BufferedReader reader = new BufferedReader(new FileReader(this.FILE_PATH));
        CsvToBean<MsgHeadquarter> cb = new CsvToBeanBuilder<MsgHeadquarter>(reader)
                // Skip Header
                .withSkipLines(1)
                .withMappingStrategy(ms)
                .build();

        List<MsgHeadquarter> list = cb.parse();
        return Objects.requireNonNullElseGet(list, ArrayList::new);
    }

    private void saveDistanceMatrix(String[][] matrix) throws IOException{
        CSVWriter csvWriter = new CSVWriter(new FileWriter(this.DISTANCE_FILE), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER,"\n");
        for(String[] arr: matrix) {
            csvWriter.writeNext(arr, false);
        }
        csvWriter.close();
    }

    private double[][] loadDistances() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.DISTANCE_FILE));
            CSVReader csvReader = new CSVReader(reader);
            List<String[]> distancesAsString = csvReader.readAll();
            double[][] distances = new double[distancesAsString.size()][distancesAsString.size()];
            int index = 0;
            for (String[] row : distancesAsString) {
                double[] parsedValues = Arrays.stream(row).mapToDouble(Double::parseDouble).toArray();
                distances[index++] = parsedValues;
            }
            return distances;
        } catch (IOException | CsvException e) {
            System.err.println(Arrays.toString(e.getStackTrace()));
            return  null;
        }
    }
}
