package model.visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe que encapsula les dades necessàries per visualitzar un graf TSP.
 * 
 * Conté informació sobre les ciutats (nodes), les arestes, i el camí òptim
 * per facilitar la renderització gràfica del problema i la seva solució.
 * 
 * @author @author Dylan Canning Garcia
 * @version 1.0
 */
public class GraphVisualizationData {
    
    private final List<City> cities;
    private final List<Edge> edges;
    private final List<Edge> optimalPath;
    private final double totalCost;
    private final boolean hasSolution;
    
    /**
     * Constructor privat. Utilitzar el Builder per crear instàncies.
     * 
     * @param builder instància del builder amb les dades configurades
     */
    private GraphVisualizationData(Builder builder) {
        this.cities = Collections.unmodifiableList(new ArrayList<>(builder.cities));
        this.edges = Collections.unmodifiableList(new ArrayList<>(builder.edges));
        this.optimalPath = Collections.unmodifiableList(new ArrayList<>(builder.optimalPath));
        this.totalCost = builder.totalCost;
        this.hasSolution = builder.hasSolution;
    }
    
    /**
     * Obté la llista de ciutats del graf.
     * 
     * @return llista immutable de ciutats
     */
    public List<City> getCities() {
        return cities;
    }
    
    /**
     * Obté la llista de totes les arestes del graf.
     * 
     * @return llista immutable d'arestes
     */
    public List<Edge> getEdges() {
        return edges;
    }
    
    /**
     * Obté la llista d'arestes que formen el camí òptim.
     * 
     * @return llista immutable d'arestes del camí òptim
     */
    public List<Edge> getOptimalPath() {
        return optimalPath;
    }
    
    /**
     * Obté el cost total del camí òptim.
     * 
     * @return cost total de la solució òptima
     */
    public double getTotalCost() {
        return totalCost;
    }
    
    /**
     * Indica si hi ha una solució disponible per visualitzar.
     * 
     * @return true si hi ha una solució òptima
     */
    public boolean hasSolution() {
        return hasSolution;
    }
    
    /**
     * Obté el nombre de ciutats del graf.
     * 
     * @return nombre de ciutats
     */
    public int getNumberOfCities() {
        return cities.size();
    }
    
    /**
     * Obté el nombre total d'arestes del graf.
     * 
     * @return nombre total d'arestes
     */
    public int getNumberOfEdges() {
        return edges.size();
    }
    
    /**
     * Troba una ciutat per el seu índex.
     * 
     * @param index índex de la ciutat
     * @return ciutat amb l'índex especificat
     * @throws IndexOutOfBoundsException si l'índex no és vàlid
     */
    public City getCityByIndex(int index) {
        return cities.stream()
                .filter(city -> city.getIndex() == index)
                .findFirst()
                .orElseThrow(() -> new IndexOutOfBoundsException("Ciutat amb índex " + index + " no trobada"));
    }
    
    /**
     * Calcula les dimensions necessàries per la visualització.
     * 
     * @return dimensions del graf
     */
    public Dimensions getGraphDimensions() {
        if (cities.isEmpty()) {
            return new Dimensions(0, 0, 600, 600);
        }
        
        double minX = cities.stream().mapToDouble(City::getX).min().orElse(0);
        double maxX = cities.stream().mapToDouble(City::getX).max().orElse(0);
        double minY = cities.stream().mapToDouble(City::getY).min().orElse(0);
        double maxY = cities.stream().mapToDouble(City::getY).max().orElse(0);
        
        // Afegir marge per a millor visualització
        double margin = 50;
        double width = (maxX - minX) + 2 * margin;
        double height = (maxY - minY) + 2 * margin;
        
        return new Dimensions(minX - margin, minY - margin, width, height);
    }
    
    /**
     * Classe interna que representa una ciutat del graf.
     */
    public static class City {
        private final int index;
        private final double x;
        private final double y;
        private final String label;
        private final boolean isStartCity;
        
        /**
         * Constructor per crear una ciutat estàndard.
         * 
         * @param index índex de la ciutat
         * @param x coordenada x
         * @param y coordenada y
         * @param label etiqueta de la ciutat
         */
        public City(int index, double x, double y, String label) {
            this(index, x, y, label, false);
        }
        
        /**
         * Constructor complet per crear una ciutat.
         * 
         * @param index índex de la ciutat
         * @param x coordenada x
         * @param y coordenada y
         * @param label etiqueta de la ciutat
         * @param isStartCity si aquesta ciutat és la ciutat inicial
         */
        public City(int index, double x, double y, String label, boolean isStartCity) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.label = label;
            this.isStartCity = isStartCity;
        }
        
        public int getIndex() { return index; }
        public double getX() { return x; }
        public double getY() { return y; }
        public String getLabel() { return label; }
        public boolean isStartCity() { return isStartCity; }
        
        @Override
        public String toString() {
            return String.format("City[%d] %s (%.1f, %.1f)", index, label, x, y);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            City city = (City) obj;
            return index == city.index;
        }
        
        @Override
        public int hashCode() {
            return Integer.hashCode(index);
        }
    }
    
    /**
     * Classe interna que representa una aresta del graf.
     */
    public static class Edge {
        private final int fromCity;
        private final int toCity;
        private final double weight;
        private final boolean isOptimal;
        
        /**
         * Constructor per crear una aresta estàndard.
         * 
         * @param fromCity ciutat d'origen
         * @param toCity ciutat de destí
         * @param weight pes de l'aresta
         */
        public Edge(int fromCity, int toCity, double weight) {
            this(fromCity, toCity, weight, false);
        }
        
        /**
         * Constructor complet per crear una aresta.
         * 
         * @param fromCity ciutat d'origen
         * @param toCity ciutat de destí
         * @param weight pes de l'aresta
         * @param isOptimal si aquesta aresta forma part del camí òptim
         */
        public Edge(int fromCity, int toCity, double weight, boolean isOptimal) {
            this.fromCity = fromCity;
            this.toCity = toCity;
            this.weight = weight;
            this.isOptimal = isOptimal;
        }
        
        public int getFromCity() { return fromCity; }
        public int getToCity() { return toCity; }
        public double getWeight() { return weight; }
        public boolean isOptimal() { return isOptimal; }
        
        @Override
        public String toString() {
            return String.format("Edge[%d → %d] weight=%.2f %s", 
                               fromCity, toCity, weight, isOptimal ? "(OPTIMAL)" : "");
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Edge edge = (Edge) obj;
            return fromCity == edge.fromCity && toCity == edge.toCity;
        }
        
        @Override
        public int hashCode() {
            return 31 * fromCity + toCity;
        }
    }
    
    /**
     * Classe interna que representa les dimensions del graf.
     */
    public static class Dimensions {
        private final double minX;
        private final double minY;
        private final double width;
        private final double height;
        
        /**
         * Constructor per crear un objecte dimensions.
         * 
         * @param minX coordenada x mínima
         * @param minY coordenada y mínima
         * @param width amplada total
         * @param height alçada total
         */
        public Dimensions(double minX, double minY, double width, double height) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
        }
        
        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public double getMaxX() { return minX + width; }
        public double getMaxY() { return minY + height; }
        
        @Override
        public String toString() {
            return String.format("Dimensions[%.1f×%.1f at (%.1f, %.1f)]", 
                               width, height, minX, minY);
        }
    }
    
    /**
     * Builder per crear instàncies de GraphVisualizationData.
     */
    public static class Builder {
        private final List<City> cities = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final List<Edge> optimalPath = new ArrayList<>();
        private double totalCost = 0;
        private boolean hasSolution = false;
        private final int expectedCities;
        
        /**
         * Constructor del builder.
         * 
         * @param expectedCities nombre esperat de ciutats
         */
        public Builder(int expectedCities) {
            this.expectedCities = expectedCities;
        }
        
        /**
         * Afegeix una ciutat al graf.
         * 
         * @param index índex de la ciutat
         * @param x coordenada x
         * @param y coordenada y
         * @param label etiqueta de la ciutat
         * @return aquest builder per encadenar mètodes
         */
        public Builder addCity(int index, double x, double y, String label) {
            cities.add(new City(index, x, y, label, index == 0));
            return this;
        }
        
        /**
         * Afegeix una aresta al graf.
         * 
         * @param from índex de la ciutat d'origen
         * @param to índex de la ciutat de destí
         * @param weight pes de l'aresta
         * @param isOptimal si l'aresta forma part del camí òptim
         * @return aquest builder per encadenar mètodes
         */
        public Builder addEdge(int from, int to, double weight, boolean isOptimal) {
            Edge edge = new Edge(from, to, weight, isOptimal);
            edges.add(edge);
            
            if (isOptimal) {
                optimalPath.add(edge);
                totalCost += weight;
                hasSolution = true;
            }
            
            return this;
        }
        
        /**
         * Afegeix una aresta sense especificar si és òptima.
         * 
         * @param from índex de la ciutat d'origen
         * @param to índex de la ciutat de destí
         * @param weight pes de l'aresta
         * @return aquest builder per encadenar mètodes
         */
        public Builder addEdge(int from, int to, double weight) {
            return addEdge(from, to, weight, false);
        }
        
        /**
         * Estableix el cost total del camí òptim manualment.
         * 
         * @param totalCost cost total
         * @return aquest builder per encadenar mètodes
         */
        public Builder setTotalCost(double totalCost) {
            this.totalCost = totalCost;
            return this;
        }
        
        /**
         * Marca que hi ha una solució disponible.
         * 
         * @param hasSolution si hi ha solució
         * @return aquest builder per encadenar mètodes
         */
        public Builder setHasSolution(boolean hasSolution) {
            this.hasSolution = hasSolution;
            return this;
        }
        
        /**
         * Construeix la instància de GraphVisualizationData.
         * 
         * @return nova instància amb les dades especificades
         * @throws IllegalStateException si no s'han proporcionat prou dades
         */
        public GraphVisualizationData build() {
            if (cities.size() != expectedCities) {
                throw new IllegalStateException(
                    String.format("S'esperaven %d ciutats, però se n'han afegit %d", 
                                expectedCities, cities.size()));
            }
            
            return new GraphVisualizationData(this);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GraphVisualizationData:\n");
        sb.append("  Ciutats: ").append(cities.size()).append("\n");
        sb.append("  Arestes: ").append(edges.size()).append("\n");
        sb.append("  Camí òptim: ").append(optimalPath.size()).append(" arestes\n");
        sb.append("  Cost total: ").append(String.format("%.2f", totalCost)).append("\n");
        sb.append("  Té solució: ").append(hasSolution);
        return sb.toString();
    }
}