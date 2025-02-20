import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class main {
    public static void main(String[] args) { // if cluster is empty it's total distance shouldn't be 0 but it should say that it's empty

        String path = "resources/iris_kmeans.txt";
        System.out.println("Enter number of clusters: ");
        Scanner sc = new Scanner(System.in);
        int k = sc.nextInt();
        DataService dataService = new DataService(path, k);

        /*System.out.println("Enter number of clusters: ");
        Scanner sc = new Scanner(System.in);
        int k = sc.nextInt();*/
        boolean run = true;
        ClusterService clusterService = new ClusterService(k, dataService);
        for(Cluster cluster : clusterService.getClusterList()){
            cluster.calculateCenter();
        }
        clusterService.getProportions();
        double fsum = 0;
        for(Cluster cluster : clusterService.getClusterList()){
            fsum += cluster.getDistanceInside();
            //fsum += cluster.calculateDistance();
        }
        System.out.println("\n" + "Total distance of all clusters: " + fsum);
        for(Cluster cluster : clusterService.getClusterList()){
            cluster.calculateCenter();
        }

        while(run){
            /*for(Cluster cluster : clusterService.getClusterList()){
                cluster.calculateCenter();
                cluster.getTotalDistance();
            }*/

            System.out.println("old centers:");
            List<List<Double>> centers = new ArrayList<>();
            for(Cluster cluster : clusterService.getClusterList()){
                if(!cluster.checkIfEmpty()) {
                    //centers.add(cluster.getCenter());
                    System.out.println("Cluster " + cluster.getId() + " center: " + cluster.getCenter());
                }else System.out.println("Cluster " + cluster.getId() + " is empty");
                centers.add(cluster.getCenter());
            }

            List<List<Observation>> redistribution = new ArrayList<>();
            for(int i = 0; i < clusterService.getClusterList().size(); i++){
                redistribution.add(new ArrayList<>());
            }


            for(Cluster cluster : clusterService.getClusterList()){
                for(Observation o : cluster.getObservationList()){
                    int id = o.calcDistance(clusterService.getClusterList());
                    redistribution.get(id).add(o);
                    //System.out.println("Added redistribution at id: " + id);
                }
            }

            clusterService.reassignObservations(redistribution);
            for(Cluster cluster : clusterService.getClusterList()){
                if(!cluster.checkIfEmpty())cluster.calculateCenter();
            }
            System.out.println("new centers:");
            for(Cluster cluster : clusterService.getClusterList()){
                if(!cluster.checkIfEmpty())System.out.println("Cluster " + cluster.getId() + " center: " + cluster.getCenter());
                else System.out.println("Cluster " + cluster.getId() + " is empty");
            }

            //clusterService.reassignObservations(redistribution);
            clusterService.getProportions();
            double sum = 0;
            for(Cluster cluster : clusterService.getClusterList()){
                sum += cluster.calculateDistance();
            }
            System.out.println("Total distance of all clusters: " + sum);
            run = !checkCenters(centers, clusterService);
        }
    }
    static boolean checkCenters(List<List<Double>> centers, ClusterService clusterService){
        List<List<Double>> newCenters = new ArrayList<>();
        for(Cluster cluster : clusterService.getClusterList()){
            newCenters.add(cluster.getCenter());
        }
        return newCenters.equals(centers);
    }
}

class DataService{
    //List<String> data;
    private int k;
    private List<String> initData;
    private List<String> spreadData;
    public DataService(String path, int k){
        //data = new ArrayList<>();
        initData = new ArrayList<>();
        spreadData = new ArrayList<>();
        this.k = k;
        setData(path);
        System.out.println("Data is: " + initData.size() + spreadData.size() + " long");
    }
    private void setData(String path){
        List<String> data;

        try{
            BufferedReader br = new BufferedReader(new FileReader(path));
            //this.data = br.lines().toList();
            data = br.lines().toList();
            this.initData = data.subList(0, k);
            this.spreadData = data.subList(k, data.size());
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public List<String> getInitData() {
        return initData;
    }

    public List<String> getSpreadData() {
        return spreadData;
    }
}

class ClusterService{
    private List<Cluster> clusterList;
    private DataService dataService;

    public ClusterService(int k, DataService dataService){
        this.dataService = dataService;
        this.clusterList = new ArrayList<>();
        for(int i = 0; i < k; i++){
             this.clusterList.add(new Cluster(clusterList.size()));
        }
        spreadData();
    }
    private void spreadData(){
        for(int i = 0; i < clusterList.size(); i ++){
            clusterList.get(i).addObservation(dataService.getInitData().get(i));
        }
        for(String observation : dataService.getSpreadData()){
            int clusterNum = (int) (Math.random() * (this.clusterList.size()));
            clusterList.get(clusterNum).addObservation(observation);
            //System.out.println("added to cluster: " + clusterNum);
        }
    }

    public void reassignObservations(List<List<Observation>> list){
        for(int i = 0; i < clusterList.size(); i++){
            clusterList.get(i).setObservationList(list.get(i));
        }
    }

    public void getProportions(){
        for(Cluster cluster : clusterList){
            Map<String, Integer> names = new HashMap<>();
            for(Observation o : cluster.getObservationList()){
                if(!names.containsKey(o.getName()))names.put(o.getName(), 1);
                else names.replace(o.getName(), names.get(o.getName()) + 1);
            }

            System.out.println("Cluster: " + cluster.getId());
            if(!cluster.checkIfEmpty())System.out.println("Total distance: " + cluster.getDistanceInside());
            else System.out.println("Cluster is empty");
            //System.out.println("Total distance: " + cluster.calculateDistance());
            for(String name : names.keySet()){
                double proportion = (double) names.get(name) / cluster.getObservationList().size();
                System.out.println(name + ": " + proportion);
            }
        }
    }

    public List<Cluster> getClusterList() {
        return clusterList;
    }
}

class Cluster{
    private List<Observation> observationList;
    private double totalDistance;
    private List<Double> center;
    private int id;
    private boolean empty;

    public Cluster(int id){
        observationList = new ArrayList<>();
        this.id = id;
    }

    public void addObservation(String observation){
        this.observationList.add(new Observation(observation));
    }

    public void setObservationList(List<Observation> list){
        observationList = list;
    }

    public void calculateCenter(){
        if(!observationList.isEmpty()) {
            int amountOfProperties = observationList.get(0).getProperties().size();
            Double[] cent = new Double[amountOfProperties];
            Arrays.fill(cent, 0.0);
            for (Observation o : observationList) {
                for (int i = 0; i < amountOfProperties; i++) {
                    cent[i] += o.getProperties().get(i);
                }
            }

            for (int i = 0; i < cent.length; i++) {
                cent[i] /= observationList.size();
            }
            this.center = Arrays.asList(cent);
        }else {
            this.empty = true;
            Double[] cent = new Double[center.size()];
            Arrays.fill(cent, 0.0);
            this.center = Arrays.asList(cent);
        }
    }

    public double getDistanceInside(){
        double sum = 0;
        for(Observation observation : observationList){
            for(int i = 0; i < observation.getProperties().size(); i++){
                sum += Math.pow(this.getCenter().get(i) - observation.getProperties().get(i), 2);
            }
        }
        return sum;
    }


    public double calculateDistance(){
        double newDistance = 0.0;
        for(Observation observ : observationList){
            newDistance += observ.getSmallestDistance();
            //System.out.println("Smallest dist: " + observ.getSmallestDistance());
        }
        this.totalDistance = newDistance;
        return newDistance;
    }

    public List<Observation> getObservationList() {
        return observationList;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public boolean checkIfEmpty() {
        return empty;
    }

    public List<Double> getCenter() {
        return center;
    }

    public void setCenter(List<Double> center) {
        this.center = center;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

class Observation{
    private List<Double> properties;
    private String name;
    private double smallestDistance;

    public Observation(String observation){
        this.properties = new ArrayList<>();
        List<String> tmp = Arrays.asList(observation.split(","));
        for(int i = 0; i < tmp.size() - 1; i++){
           this.properties.add(Double.parseDouble(tmp.get(i)));
        }
        this.name = tmp.getLast();
    }
    /*public int calcDistance(List<Cluster> clusters){
        int sum = 0;

    }*/
    public int calcDistance(List<Cluster> clusters){
        double lowDistance = Double.MAX_VALUE;
        int lowId = -1;
        for(Cluster cluster : clusters){
            double distance = 0;
            for(int i = 0; i < properties.size(); i++){
                distance += Math.pow(cluster.getCenter().get(i) - properties.get(i), 2);
            }
            distance = Math.sqrt(distance);
            if(distance <= lowDistance){
                lowDistance = distance;
                lowId = cluster.getId();
            }
        }smallestDistance = lowDistance;
        return lowId;
    }

    public List<Double> getProperties() {
        return properties;
    }

    public void setProperties(List<Double> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSmallestDistance() {
        return smallestDistance;
    }

    public void setSmallestDistance(double smallestDistance) {
        this.smallestDistance = smallestDistance;
    }
}

