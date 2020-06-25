import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import com.opencsv.exceptions.CsvException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.utils.URIBuilder;

import com.google.common.primitives.Booleans;

import org.json.JSONObject;

import javax.imageio.ImageIO;


public class Msg_tsp {

    private final String FILE_PATH;
    private final String DISTANCE_FILE ="src/main/resources/distances.csv";
    private final String OSRM_LINK = "http://localhost:5000/";
    private List<MsgHeadquarter> hqs;

    Msg_tsp(String filePath) {
        this.FILE_PATH = filePath;
        hqs = this.loadHQ();
    }

    void solveTsp(boolean recalculateDistances) {
        if(recalculateDistances) {
            this.recalculateDistances();
        }

        double[][] distances = this.loadDistances();
        // Ismaning is the first row in the file
        int indexOfStartHq = 0;
        nearestNeighbor(distances, indexOfStartHq);
        greedy(distances);
    }

    void nearestNeighbor(double[][] distances, int startIndex) {
        // Init the route
        boolean[] cityVisited = new boolean[distances.length];
        List<Integer> route = new LinkedList<>();
        route.add(startIndex);
        cityVisited[startIndex] = true;
        int currentCity = startIndex;
        // Start the algorithm
        while(Booleans.asList(cityVisited).contains(false)) {
            currentCity = this.findNextUnvisitedCity(distances, currentCity, cityVisited);
            route.add(currentCity);
            cityVisited[currentCity] = true;
        }
        // Go back to the start city
        route.add(startIndex);
        System.out.println(this.describeRoute(route));
        double length = this.calculateDistanceOfRoute(distances, route);
        this.drawMap("NearestNeighbour.png", route, "Nearest neighbour", length);
    }

    void greedy(double[][] distances) {
        // Sort the edges in increasing order of the weights
        // Choose the smallest one which
        // 1. does not cause a vertex to have a degreed of three or more
        // 2. does not form a cycle
        boolean[][] alreadySelectedEdge = new boolean[distances.length][distances.length];
        // mark the diagonal as used because it is the same node
        for(int i = 0; i < alreadySelectedEdge.length; i++) {
            alreadySelectedEdge[i][i] = true;
        }
        // init list
        PriorityQueue<Node> nodelist = new PriorityQueue<>();
        for(int i = 0; i <alreadySelectedEdge.length; i++) {
            for(int j = i; j< alreadySelectedEdge.length; j++) {
                if(!alreadySelectedEdge[i][j]){
                    Node node = new Node(i,j, distances[i][j]);
                    nodelist.add(node);
                }
            }
        }
        List<Node> selectedList = new LinkedList<>();
        Node firstNode = nodelist.poll();
        selectedList.add(firstNode);
        alreadySelectedEdge[firstNode.getFrom()][firstNode.getTo()] = true;
        alreadySelectedEdge[firstNode.getTo()][firstNode.getFrom()] = true;

        while(selectedList.size() < distances.length) {
            Node nextNode = nodelist.poll();
            alreadySelectedEdge[nextNode.getFrom()][nextNode.getTo()] = true;
            alreadySelectedEdge[nextNode.getTo()][nextNode.getFrom()] = true;
            boolean[] neighboursTo = alreadySelectedEdge[nextNode.getTo()];
            boolean[] neighboursFrom = alreadySelectedEdge[nextNode.getFrom()];
            System.out.println("outer loop");
            System.out.println(selectedList.size());
            // Check conditions
            // It is four because in the matrix the distance from the node to his self is marked as selected
            while(Booleans.asList(neighboursTo).stream().filter(b -> b).count() >= 4 || Booleans.asList(neighboursFrom).stream().filter(b -> b).count() >= 4){
                nextNode = nodelist.poll();
                //alreadySelectedEdge[nextNode.getFrom()][nextNode.getTo()] = true;
                //alreadySelectedEdge[nextNode.getTo()][nextNode.getFrom()] = true;
                neighboursTo = alreadySelectedEdge[nextNode.getTo()];
                neighboursFrom = alreadySelectedEdge[nextNode.getFrom()];
            }
            selectedList.add(nextNode);
            alreadySelectedEdge[nextNode.getFrom()][nextNode.getTo()] = true;
            alreadySelectedEdge[nextNode.getTo()][nextNode.getFrom()] = true;
        }
        System.out.println(selectedList);
        this.drawSingleRoutes("greedy.png", selectedList);
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

        for(int cityIndex : route) {
            cities.append(hqs.get(cityIndex).getName());
            cities.append(" -> ");
        }

        return cities.toString();
    }

    void recalculateDistances() {
        try{
            String[][] distanceMatrix = new String[hqs.size()][hqs.size()];
            for(int i = 0; i < hqs.size(); i++) {
                for(int j = 0; j <=i; j++) {
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
                        // The distance between the same points is 0.0
                        distanceMatrix[i][j] = "0.0";
                    }
                }
            }
            saveDistanceMatrix(distanceMatrix);
        } catch (IOException iox) {
            System.err.println("IOException occurred");
            System.err.println(Arrays.toString(iox.getStackTrace()));
        } catch(URISyntaxException uriSyntaxException) {
            System.err.println("URISyntaxException occurred");
            System.err.println(Arrays.toString(uriSyntaxException.getStackTrace()));
        }
    }

    List<MsgHeadquarter> loadHQ() {
        ColumnPositionMappingStrategy<MsgHeadquarter> ms = new ColumnPositionMappingStrategy<>();
        ms.setType(MsgHeadquarter.class);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.FILE_PATH));
            CsvToBean<MsgHeadquarter> cb = new CsvToBeanBuilder<MsgHeadquarter>(reader)
                    // Skip Header
                    .withSkipLines(1)
                    .withMappingStrategy(ms)
                    .build();

            List<MsgHeadquarter> list = cb.parse();
            return Objects.requireNonNullElseGet(list, ArrayList::new);
        } catch (IOException e) {
            System.err.println("Error during reading HQ File: ");
            System.err.println(Arrays.toString(e.getStackTrace()));
        }
        return  null;
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

    private void drawMap(String fileName, List<Integer> route, String algorithmName, double length) {
        // Extracted from https://de.wikipedia.org/wiki/Liste_der_Extrempunkte_Deutschlands
        // There are used to estimate the position of the cities on the map
        double highestLatitudeOfGer = 54.91131;
        double smallestLatitudeOfGer = 47.271679;
        double diffLatitude = highestLatitudeOfGer - smallestLatitudeOfGer;
        double highestLongitudeGer = 15.043611;
        double smallestLongitudeGer = 5.866944;
        double diffLongitude = highestLongitudeGer - smallestLongitudeGer;
        double lastX = 0.0;
        double lastY = 0.0;

        try {
            String mapUrl = "src/main/resources/Germany_location_map.png";
            BufferedImage mapImage = ImageIO.read(new File(mapUrl));
            int width = mapImage.getWidth();
            int height = mapImage.getHeight();

            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = newImage.getGraphics();
            g.drawImage(mapImage,0,0,null);
            g.setColor(Color.RED);
            g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
            for(int hqIndex : route) {
                double x = (hqs.get(hqIndex).getLongitude() -smallestLongitudeGer) * (width/diffLongitude);
                double y = height - (hqs.get(hqIndex).getLatitude() - smallestLatitudeOfGer) * ( height / diffLatitude);
                g.fillOval((int)(x-12.5),(int)(y-12.5),25, 25);
                g.drawString(hqs.get(hqIndex).getName(),(int)x-40,(int)y-13);
                if(lastX != 0.0 && lastY != 0.0) {
                    g.drawLine((int)x,(int)y,(int)lastX, (int)lastY);
                }
                lastX = x;
                lastY = y;
            }
            g.setFont(new Font("TimesRoman", Font.PLAIN, 20));
            g.drawString(algorithmName, 50,50);
            g.drawString("Total length: " + length + " meters", 50,100);

            ImageIO.write(newImage, "png", new File("src/main/resources/" + fileName));
        } catch (IOException ioException) {
            System.err.println(Arrays.toString(ioException.getStackTrace()));
        }
    }

    private void drawSingleRoutes(String fileName, List<Node> nodes) {
        double highestLatitudeOfGer = 54.91131;
        double smallestLatitudeOfGer = 47.271679;
        double diffLatitude = highestLatitudeOfGer - smallestLatitudeOfGer;
        double highestLongitudeGer = 15.043611;
        double smallestLongitudeGer = 5.866944;
        double diffLongitude = highestLongitudeGer - smallestLongitudeGer;
        double lastX = 0.0;
        double lastY = 0.0;

        try {
            String mapUrl = "src/main/resources/Germany_location_map.png";
            BufferedImage mapImage = ImageIO.read(new File(mapUrl));
            int width = mapImage.getWidth();
            int height = mapImage.getHeight();

            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = newImage.getGraphics();
            g.drawImage(mapImage,0,0,null);
            g.setColor(Color.RED);
            g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
            for(Node node : nodes) {
                double x = (hqs.get(node.getFrom()).getLongitude() -smallestLongitudeGer) * (width/diffLongitude);
                double y = height - (hqs.get(node.getFrom()).getLatitude() - smallestLatitudeOfGer) * ( height / diffLatitude);
                g.fillOval((int)(x-12.5),(int)(y-12.5),25, 25);
                g.drawString(hqs.get(node.getFrom()).getName(),(int)x-40,(int)y-13);

                double x2 = (hqs.get(node.getTo()).getLongitude() -smallestLongitudeGer) * (width/diffLongitude);
                double y2 = height - (hqs.get(node.getTo()).getLatitude() - smallestLatitudeOfGer) * ( height / diffLatitude);
                g.fillOval((int)(x2-12.5),(int)(y2-12.5),25, 25);
                g.drawString(hqs.get(node.getTo()).getName(),(int)x2-40,(int)y2-13);

                g.drawLine((int)x,(int)y,(int)x2, (int)y2);

            }
            g.setFont(new Font("TimesRoman", Font.PLAIN, 20));

            ImageIO.write(newImage, "png", new File("src/main/resources/" + fileName));
        } catch (IOException ioException) {
            System.err.println(Arrays.toString(ioException.getStackTrace()));
        }
    }
}
