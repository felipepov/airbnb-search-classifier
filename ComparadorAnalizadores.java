import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.*;
import java.util.*;

//Compilar el programa Java
// javac -cp "lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar:lucene-10.3.1/modules/lucene-suggest-10.3.1.jar" ComparadorAnalizadores.java

// Ejecutar el programa
//  java -cp ".:lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar:lucene-10.3.1/modules/lucene-suggest-10.3.1.jar" ComparadorAnalizadores


/**
 * Comparador de Analizadores de Lucene para Airbnb Listings
 * 
 * Este programa compara diferentes analizadores de Lucene aplicados a los campos
 * 'amenities' y 'host_neighbourhood' del archivo listings.csv para determinar
 * cuál proporciona la mejor experiencia de usuario en búsquedas.
 * 
 * Objetivo: Evaluar analizadores para encontrar el mejor balance entre
 * precisión, recall y experiencia de usuario en búsquedas de propiedades.
 */
public class ComparadorAnalizadores {
    
    // Configuración de archivos
    private static final String CSV_FILE = "listings.csv";
    private static final int MAX_SAMPLES = 10; // Número de muestras a analizar
    
    // Consultas de prueba para evaluar sugerencias
    private static final String[] CONSULTAS_PRUEBA = {
        "wifi", "pool", "kitchen", "parking", "gym", "beach", "downtown", "hollywood", 
        "santa monica", "air conditioning", "balcony", "garden", "pet friendly"
    };
    
    public static void main(String[] args) throws IOException {
        System.out.println("=== COMPARADOR DE ANALIZADORES LUCENE PARA AIRBNB ===\n");
        
        // Leer muestras del CSV
        List<String[]> samples = leerMuestrasCSV(CSV_FILE, MAX_SAMPLES);
        
        if (samples.isEmpty()) {
            System.err.println("No se pudieron leer muestras del archivo CSV");
            return;
        }
        
        // Mostrar información de las muestras
        mostrarInformacionMuestras(samples);
        
        // Comparar diferentes analizadores
        compararAnalizadores(samples);
        
        // Evaluar calidad de sugerencias
        System.out.println("\n=== EVALUACIÓN DE SUGERENCIAS ===");
        evaluarSugerencias(samples);
        
        System.out.println("\n=== ANÁLISIS COMPLETADO ===");
    }
    
    /**
     * Lee muestras del archivo CSV
     */
    private static List<String[]> leerMuestrasCSV(String filename, int maxSamples) throws IOException {
        List<String[]> samples = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String header = br.readLine(); // Saltar encabezado
            if (header == null) return samples;
            
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count < maxSamples) {
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split respetando comillas
                if (fields.length > 40) { // Verificar que tenga suficientes campos
                    samples.add(fields);
                    count++;
                }
            }
        }
        
        return samples;
    }
    
    /**
     * Muestra información sobre las muestras leídas
     */
    private static void mostrarInformacionMuestras(List<String[]> samples) {
        System.out.println("Muestras leídas: " + samples.size());
        System.out.println("Campos a analizar: amenities (inglés), host_neighbourhood (nombres de lugares)\n");
        
        // Mostrar algunas muestras
        for (int i = 0; i < Math.min(3, samples.size()); i++) {
            String[] sample = samples.get(i);
            System.out.println("--- MUESTRA " + (i+1) + " ---");
            System.out.println("Amenities: " + truncarTexto(sample[39], 100));
            System.out.println("Host Neighbourhood: " + truncarTexto(sample[21], 50));
            System.out.println();
        }
    }
    
    /**
     * Compara diferentes analizadores aplicados a AMBOS campos
     */
    private static void compararAnalizadores(List<String[]> samples) throws IOException {
        
        // 1. KEYWORD ANALYZER
        System.out.println("1. KEYWORD ANALYZER");
        System.out.println("===================");
        analizarConAnalizador(samples, new KeywordAnalyzer(), "KeywordAnalyzer");
        
        // 2. STANDARD ANALYZER
        System.out.println("\n2. STANDARD ANALYZER");
        System.out.println("====================");
        analizarConAnalizador(samples, new StandardAnalyzer(), "StandardAnalyzer");
        
        // 3. ENGLISH ANALYZER
        System.out.println("\n3. ENGLISH ANALYZER");
        System.out.println("==================");
        analizarConAnalizador(samples, new EnglishAnalyzer(), "EnglishAnalyzer");
    }
    
    /**
     * Analiza muestras con un analizador específico aplicado a AMBOS campos
     */
    private static void analizarConAnalizador(List<String[]> samples, Analyzer analyzer, String nombreAnalizador) throws IOException {
        System.out.println("Analizador: " + nombreAnalizador);
        System.out.println("Características: " + obtenerCaracteristicas(analyzer));
        
        int totalTokensAmenities = 0;
        int totalTokensNeighbourhood = 0;
        int muestrasAnalizadas = 0;
        
        for (String[] sample : samples) {
            if (sample.length > 40) {
                String amenities = sample[39]; // Campo amenities
                String hostNeighbourhood = sample[21]; // Campo host_neighbourhood
                
                if (!amenities.isEmpty() && !hostNeighbourhood.isEmpty()) {
                    int tokensAmenities = analizarTexto(amenities, analyzer);
                    int tokensNeighbourhood = analizarTexto(hostNeighbourhood, analyzer);
                    
                    totalTokensAmenities += tokensAmenities;
                    totalTokensNeighbourhood += tokensNeighbourhood;
                    muestrasAnalizadas++;
                }
            }
        }
        
        System.out.println("Muestras analizadas: " + muestrasAnalizadas);
        System.out.println("--- RESULTADOS POR CAMPO ---");
        System.out.println("Amenities - Total tokens: " + totalTokensAmenities + 
                          " | Promedio: " + (muestrasAnalizadas > 0 ? totalTokensAmenities / muestrasAnalizadas : 0));
        System.out.println("Host Neighbourhood - Total tokens: " + totalTokensNeighbourhood + 
                          " | Promedio: " + (muestrasAnalizadas > 0 ? totalTokensNeighbourhood / muestrasAnalizadas : 0));
        System.out.println("TOTAL - Tokens: " + (totalTokensAmenities + totalTokensNeighbourhood) + 
                          " | Promedio: " + (muestrasAnalizadas > 0 ? (totalTokensAmenities + totalTokensNeighbourhood) / muestrasAnalizadas : 0));
    }
    
    
    /**
     * Analiza un texto con un analizador específico
     */
    private static int analizarTexto(String texto, Analyzer analyzer) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream("field", new StringReader(texto));
        CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
        
        tokenStream.reset();
        int count = 0;
        while (tokenStream.incrementToken()) {
            count++;
        }
        tokenStream.end();
        tokenStream.close();
        
        return count;
    }
    
    
    /**
     * Obtiene características de un analizador
     */
    private static String obtenerCaracteristicas(Analyzer analyzer) {
        if (analyzer instanceof StandardAnalyzer) {
            return "Tokenización estándar + normalización + stop words";
        } else if (analyzer instanceof EnglishAnalyzer) {
            return "Tokenización + stop words en inglés + stemming";
        } else if (analyzer instanceof KeywordAnalyzer) {
            return "Texto completo como un solo token";
        }
        return "Analizador personalizado";
    }
    
    /**
     * Trunca texto para mostrar
     */
    private static String truncarTexto(String texto, int maxLength) {
        if (texto == null) return "";
        if (texto.length() <= maxLength) return texto;
        return texto.substring(0, maxLength) + "...";
    }
    
    /**
     * Evalúa la calidad de sugerencias para diferentes analizadores
     */
    private static void evaluarSugerencias(List<String[]> samples) throws IOException {
        System.out.println("Evaluando calidad de sugerencias con consultas de usuario reales...\n");
        
        // Preparar datos para sugerencias
        Map<String, Long> amenitiesData = prepararDatosAmenities(samples);
        Map<String, Long> neighbourhoodData = prepararDatosNeighbourhood(samples);
        
        // Evaluar los 3 analizadores seleccionados
        evaluarAnalizadorSugerencias("KeywordAnalyzer", new KeywordAnalyzer(), amenitiesData, neighbourhoodData);
        evaluarAnalizadorSugerencias("StandardAnalyzer", new StandardAnalyzer(), amenitiesData, neighbourhoodData);
        evaluarAnalizadorSugerencias("EnglishAnalyzer", new EnglishAnalyzer(), amenitiesData, neighbourhoodData);
    }
    
    /**
     * Prepara datos de amenities para sugerencias
     */
    private static Map<String, Long> prepararDatosAmenities(List<String[]> samples) {
        Map<String, Long> amenitiesData = new HashMap<>();
        
        for (String[] sample : samples) {
            if (sample.length > 40 && !sample[39].isEmpty()) {
                String amenities = sample[39];
                // Limpiar y procesar amenities
                String[] amenityList = amenities.replaceAll("[\\[\\]\"]", "").split(",");
                for (String amenity : amenityList) {
                    String cleanAmenity = amenity.trim().toLowerCase();
                    if (!cleanAmenity.isEmpty()) {
                        amenitiesData.put(cleanAmenity, amenitiesData.getOrDefault(cleanAmenity, 0L) + 1);
                    }
                }
            }
        }
        
        return amenitiesData;
    }
    
    /**
     * Prepara datos de neighbourhood para sugerencias
     */
    private static Map<String, Long> prepararDatosNeighbourhood(List<String[]> samples) {
        Map<String, Long> neighbourhoodData = new HashMap<>();
        
        for (String[] sample : samples) {
            if (sample.length > 40 && !sample[21].isEmpty()) {
                String neighbourhood = sample[21].trim().toLowerCase();
                if (!neighbourhood.isEmpty()) {
                    neighbourhoodData.put(neighbourhood, neighbourhoodData.getOrDefault(neighbourhood, 0L) + 1);
                }
            }
        }
        
        return neighbourhoodData;
    }
    
    /**
     * Evalúa un analizador específico para sugerencias
     */
    private static void evaluarAnalizadorSugerencias(String nombreAnalizador, Analyzer analyzer, 
                                                   Map<String, Long> amenitiesData, Map<String, Long> neighbourhoodData) throws IOException {
        System.out.println("\n--- EVALUANDO " + nombreAnalizador + " ---");
        
        // Crear suggester para amenities
        Directory amenitiesDir = new RAMDirectory();
        AnalyzingSuggester amenitiesSuggester = new AnalyzingSuggester(amenitiesDir, "amenities", 
            analyzer, analyzer, AnalyzingSuggester.PRESERVE_SEP, 256, -1, true);
        amenitiesSuggester.build(new QueryFreqIterator(amenitiesData));
        
        // Crear suggester para neighbourhood
        Directory neighbourhoodDir = new RAMDirectory();
        AnalyzingSuggester neighbourhoodSuggester = new AnalyzingSuggester(neighbourhoodDir, "neighbourhood", 
            analyzer, analyzer, AnalyzingSuggester.PRESERVE_SEP, 256, -1, true);
        neighbourhoodSuggester.build(new QueryFreqIterator(neighbourhoodData));
        
        // Evaluar consultas de prueba
        System.out.println("Evaluando consultas de amenities:");
        evaluarConsultas(amenitiesSuggester, "amenities");
        
        System.out.println("\nEvaluando consultas de neighbourhood:");
        evaluarConsultas(neighbourhoodSuggester, "neighbourhood");
        
        // Cerrar recursos
        amenitiesDir.close();
        neighbourhoodDir.close();
    }
    
    /**
     * Evalúa consultas específicas con un suggester
     */
    private static void evaluarConsultas(AnalyzingSuggester suggester, String tipo) {
        for (String consulta : CONSULTAS_PRUEBA) {
            try {
                List<Lookup.LookupResult> sugerencias = suggester.lookup(consulta, false, 5);
                System.out.println("Consulta: '" + consulta + "' -> " + sugerencias.size() + " sugerencias");
                
                for (Lookup.LookupResult resultado : sugerencias) {
                    System.out.println("  - " + resultado.key + " (peso: " + resultado.value + ")");
                }
                
                if (sugerencias.isEmpty()) {
                    System.out.println("  ⚠️  Sin sugerencias para: " + consulta);
                }
                
            } catch (IOException e) {
                System.out.println("Error evaluando consulta: " + consulta);
            }
        }
    }
    
    /**
     * Iterator para pares query-frecuencia
     */
    static class QueryFreqIterator implements InputIterator {
        private Iterator<Map.Entry<String, Long>> iterator;
        private Map.Entry<String, Long> current;
        
        public QueryFreqIterator(Map<String, Long> terms) {
            this.iterator = terms.entrySet().iterator();
        }
        
        @Override
        public long weight() {
            return current.getValue();
        }
        
        @Override
        public BytesRef next() {
            if (iterator.hasNext()) {
                current = iterator.next();
                return new BytesRef(current.getKey());
            }
            return null;
        }
        
        @Override
        public boolean hasPayloads() { return false; }
        
        @Override
        public boolean hasContexts() { return false; }
        
        @Override
        public Set<BytesRef> contexts() { return null; }
        
        @Override
        public BytesRef payload() { return null; }
    }
}
