public class Main {

    public static void main(String[] args) {
        String filePath = "src/main/resources/msg_standorte_deutschland.csv";
        Msg_tsp tsp = new Msg_tsp(filePath);
        tsp.solveTsp(false);
    }
}
