import com.opencsv.bean.CsvBindByPosition;

public class MsgHeadquarter {
    @CsvBindByPosition(position=0)
    private int number;
    @CsvBindByPosition(position=1)
    private String name;
    @CsvBindByPosition(position=2)
    private String street;
    @CsvBindByPosition(position=3)
    private String houseNumber;
    @CsvBindByPosition(position=4)
    private int zip;
    @CsvBindByPosition(position=5)
    private String city;
    @CsvBindByPosition(position=6)
    private double latitude;
    @CsvBindByPosition(position=7)
    private double longitude;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getName() {return name; }
}
