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
import java.util.stream.Collectors;

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
    private final List<MsgHeadquarter> hqs;

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

        while(selectedList.size() < distances.length) {
            Node nextNode = nodelist.poll();
            boolean[][] copiedSelected = deepCopy(alreadySelectedEdge);
            copiedSelected[nextNode.getFrom()][nextNode.getTo()] = true;
            copiedSelected[nextNode.getTo()][nextNode.getFrom()] = true;
            boolean[] neighboursTo = copiedSelected[nextNode.getTo()];
            boolean[] neighboursFrom = copiedSelected[nextNode.getFrom()];

            List<Node> copiedSelectedList = new LinkedList<>(selectedList);
            copiedSelectedList.add(nextNode);
            // Check conditions
            // Choose the smallest edge which
            // 1. does not cause a vertex to have a degree of three or more
            // 2. does not form a cycle except the travel man visited all cities
            while(Booleans.asList(neighboursTo).stream().filter(b -> b).count() > 3 || Booleans.asList(neighboursFrom).stream().filter(b -> b).count() > 3
                || (isCyclic(distances.length, copiedSelectedList) && copiedSelectedList.size() < distances.length)){
                nextNode = nodelist.poll();
                copiedSelected = deepCopy(alreadySelectedEdge);
                copiedSelected[nextNode.getFrom()][nextNode.getTo()] = true;
                copiedSelected[nextNode.getTo()][nextNode.getFrom()] = true;
                neighboursTo = copiedSelected[nextNode.getTo()];
                neighboursFrom = copiedSelected[nextNode.getFrom()];
                copiedSelectedList = new LinkedList<>(selectedList);
                copiedSelectedList.add(nextNode);
            }
            // Mark the edges of the selected node
            selectedList.add(nextNode);
            alreadySelectedEdge[nextNode.getFrom()][nextNode.getTo()] = true;
            alreadySelectedEdge[nextNode.getTo()][nextNode.getFrom()] = true;

        }
        System.out.println(selectedList);
        List<Integer>[] adj = this.createGraph(selectedList, distances.length);
        List<Integer> route = new LinkedList<>();
        // Start is in Ismaringen
        route.add(0);
        boolean[] visited = new boolean[distances.length];
        visited[0] = true;
        int index = 0;
        for(List<Integer> tmp: adj) {
            System.out.print((hqs.get(index).getNumber()-1) +  " " + hqs.get(index++).getName());
            System.out.println(tmp);
        }

        int next = adj[0].get(0);
        int count = 1;
        while(count < distances.length) {
            route.add(next);
            visited[next] = true;
            int nextZero = adj[next].get(0);
            System.out.println("Next zero:" + nextZero);
            if(!visited[nextZero]){
                next = adj[next].get(0);
            }else {
                next = adj[next].get(1);
            }
            count++;
        }
        // Go back home
        route.add(0);
        System.out.println(route);
        double length = this.calculateDistanceOfRoute(distances, route);
        this.drawMap("greedy.png", route, "greedy", length);

    }

    // Copies a two dimensional array
    boolean[][] deepCopy(boolean[][] original) {
        if(original == null) {
            return null;
        }
        boolean[][] result = new boolean[original.length][];
        for(int index = 0; index < original.length; index++) {
            result[index] = original[index].clone();
        }
        return result;
    }

    private List<Integer>[] createGraph(List<Node> nodes, int numberVertix) {
        LinkedList<Integer>[] adj = new LinkedList[numberVertix];
        for(int i =0; i<numberVertix; i++) {
            adj[i] = new LinkedList<>();
        }
        for(Node node : nodes) {
            adj[node.getTo()].add(node.getFrom());
            adj[node.getFrom()].add(node.getTo());
        }
        return adj;
    }

    boolean isCyclic(int numberVertix, List<Node> nodes) {
         List<Integer>[] adj = createGraph(nodes, numberVertix);

         boolean[] visited = new boolean[numberVertix];
         for (int i = 0; i < numberVertix; i++) {
             if(!visited[i]) {
                 if (isCyclicUtil(i, visited, -1, adj)) {
                     return true;
                 }
             }
         }
         return false;
    }

    boolean isCyclicUtil(int v, boolean[] visited, int parent, List<Integer>[] adj) {
        visited[v] = true;
        int i;

        for (Integer integer : adj[v]) {
            i = integer;
            if (!visited[i]) {
                if (isCyclicUtil(i, visited, v, adj)) {
                    return true;
                }
            } else if (i != parent) {
                return true;
            }
        }
        return  false;
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

    private long calculateDistanceOfRoute(double[][] distances, List<Integer> route) {
        double distance = 0.0;
        for(int index = 1; index < route.size(); index++) {
            System.out.println(distances[route.get(index-1)][route.get(index)]);
            distance += distances[route.get(index-1)][route.get(index)];
        }
        distance = distance/1000;
        return  Math.round(distance);
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
            g.drawString(algorithmName, 20,50);
            g.drawString("Total length: " + length + " kilometers", 20,80);

            ImageIO.write(newImage, "png", new File("src/main/resources/" + fileName));
        } catch (IOException ioException) {
            System.err.println(Arrays.toString(ioException.getStackTrace()));
        }
    }
}
