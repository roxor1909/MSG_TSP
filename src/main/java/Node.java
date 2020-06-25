public class Node implements Comparable<Node>{

    private int from;
    private int to;
    private double distance;

    public Node(int from, int to, double distance) {
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }

    public String toString() {
        return "From " + this.from + " to " + this.to;
    }

    @Override
    public int compareTo(Node o) {
        return Double.compare(this.distance, o.getDistance());
    }
}
