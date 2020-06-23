import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;



public class Msg_tsp {

    private String filePath;
    private String DISTANCE_FILE ="distances.csv";
    private String OSRM_LINK = "localhost:5000/";

    Msg_tsp(String filePath) {
        this.filePath = filePath;
        this.file = new File(this.filePath);
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
            for(int i = 0; i <= hqs.size(); i++) {
                for(int j = 0; j <=i; j++) {
                    // The distance between the same points is 0.0
                    if(i != j) {
                        MsgHeadquarter hqOne = hqs.get(i);
                        MsgHeadquarter hqTwo = hqs.get(j);
                        // Service is using Longitude, latitude
                        String coordinates =
                        URL url = new URL(this.OSRM_LINK + "route/v1/driving/")

                    }
                }
            }
        } catch (IOException iox) {
            System.out.println("IOException occured");
            System.out.println(iox.fillInStackTrace());
        }
    }

    List<MsgHeadquarter> loadHQ() throws IOException{
        ColumnPositionMappingStrategy<MsgHeadquarter> ms = new ColumnPositionMappingStrategy<MsgHeadquarter>();
        ms.setType(MsgHeadquarter.class);
        BufferedReader reader = new BufferedReader(new FileReader(this.filePath));
        CsvToBean<MsgHeadquarter> cb = new CsvToBeanBuilder<MsgHeadquarter>(reader)
                // Skip Header
                .withSkipLines(1)
                .withMappingStrategy(ms)
                .build();

        List<MsgHeadquarter> list = cb.parse();
        if(list != null) {
            return  list;
        } else {
            return new ArrayList<MsgHeadquarter>();
        }
    }

}
