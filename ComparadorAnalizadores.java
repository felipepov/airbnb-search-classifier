import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.morfologik.MorfologikAnalyzer;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.*;
import java.util.*;

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
     * Compara diferentes analizadores
     */
    private static void compararAnalizadores(List<String[]> samples) throws IOException {
        
        // 1. ANALIZADOR ESTÁNDAR (baseline)
        System.out.println("1. ANALIZADOR ESTÁNDAR (Baseline)");
        System.out.println("=====================================");
        analizarConAnalizador(samples, new StandardAnalyzer(), "StandardAnalyzer");
        
        // 2. ANALIZADOR INGLÉS (para contenido en inglés - más apropiado)
        System.out.println("\n2. ANALIZADOR INGLÉS");
        System.out.println("===================");
        analizarConAnalizador(samples, new EnglishAnalyzer(), "EnglishAnalyzer");
        
        // 3. ANALIZADOR SIMPLE (para nombres propios y términos técnicos)
        System.out.println("\n3. ANALIZADOR SIMPLE");
        System.out.println("===================");
        analizarConAnalizador(samples, new SimpleAnalyzer(), "SimpleAnalyzer");
        
        // 4. ANALIZADOR CON STEMMING (Snowball - Inglés)
        System.out.println("\n4. ANALIZADOR CON STEMMING (Snowball - Inglés)");
        System.out.println("=============================================");
        analizarConAnalizador(samples, new SnowballAnalyzer("English"), "SnowballAnalyzer(English)");
        
        // 5. ANALIZADOR KEYWORD (para búsquedas exactas)
        System.out.println("\n5. ANALIZADOR KEYWORD (búsquedas exactas)");
        System.out.println("=======================================");
        analizarConAnalizador(samples, new KeywordAnalyzer(), "KeywordAnalyzer");
        
        // 6. ANALIZADOR CON LEMATIZACIÓN (Morfologik)
        System.out.println("\n6. ANALIZADOR CON LEMATIZACIÓN (Morfologik)");
        System.out.println("==========================================");
        analizarConAnalizador(samples, new MorfologikAnalyzer(), "MorfologikAnalyzer");
        
        // 7. ANALIZADOR PERSONALIZADO CON N-GRAMAS
        System.out.println("\n7. ANALIZADOR PERSONALIZADO CON N-GRAMAS");
        System.out.println("=========================================");
        analizarConAnalizadorPersonalizado(samples);
        
        // 8. ANALIZADOR POR CAMPO (PerFieldAnalyzerWrapper)
        System.out.println("\n8. ANALIZADOR POR CAMPO (PerFieldAnalyzerWrapper)");
        System.out.println("================================================");
        analizarConAnalizadorPorCampo(samples);
    }
    
    /**
     * Analiza muestras con un analizador específico
     */
    private static void analizarConAnalizador(List<String[]> samples, Analyzer analyzer, String nombreAnalizador) throws IOException {
        System.out.println("Analizador: " + nombreAnalizador);
        System.out.println("Características: " + obtenerCaracteristicas(analyzer));
        
        int totalTokens = 0;
        int muestrasAnalizadas = 0;
        
        for (String[] sample : samples) {
            if (sample.length > 40) {
                String amenities = sample[39]; // Campo amenities
                String hostNeighbourhood = sample[21]; // Campo host_neighbourhood
                
                if (!amenities.isEmpty() && !hostNeighbourhood.isEmpty()) {
                    int tokensAmenities = analizarTexto(amenities, analyzer);
                    int tokensNeighbourhood = analizarTexto(hostNeighbourhood, analyzer);
                    
                    totalTokens += tokensAmenities + tokensNeighbourhood;
                    muestrasAnalizadas++;
                }
            }
        }
        
        System.out.println("Muestras analizadas: " + muestrasAnalizadas);
        System.out.println("Total de tokens: " + totalTokens);
        System.out.println("Promedio de tokens por muestra: " + (muestrasAnalizadas > 0 ? totalTokens / muestrasAnalizadas : 0));
    }
    
    /**
     * Analiza con analizador personalizado que incluye n-gramas
     */
    private static void analizarConAnalizadorPersonalizado(List<String[]> samples) throws IOException {
        System.out.println("Analizador: Personalizado con N-gramas");
        System.out.println("Características: StandardTokenizer + LowerCaseFilter + StopFilter + EdgeNGramFilter");
        
        int totalTokens = 0;
        int muestrasAnalizadas = 0;
        
        for (String[] sample : samples) {
            if (sample.length > 40) {
                String amenities = sample[39];
                String hostNeighbourhood = sample[21];
                
                if (!amenities.isEmpty() && !hostNeighbourhood.isEmpty()) {
                    int tokensAmenities = analizarConNGramas(amenities);
                    int tokensNeighbourhood = analizarConNGramas(hostNeighbourhood);
                    
                    totalTokens += tokensAmenities + tokensNeighbourhood;
                    muestrasAnalizadas++;
                }
            }
        }
        
        System.out.println("Muestras analizadas: " + muestrasAnalizadas);
        System.out.println("Total de tokens: " + totalTokens);
        System.out.println("Promedio de tokens por muestra: " + (muestrasAnalizadas > 0 ? totalTokens / muestrasAnalizadas : 0));
    }
    
    /**
     * Analiza con analizador por campo usando PerFieldAnalyzerWrapper
     */
    private static void analizarConAnalizadorPorCampo(List<String[]> samples) throws IOException {
        System.out.println("Analizador: PerFieldAnalyzerWrapper");
        System.out.println("Características: Diferentes analizadores para diferentes campos");
        
        // Configurar analizadores por campo
        Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        fieldAnalyzers.put("amenities", new EnglishAnalyzer()); // Para amenities en inglés
        fieldAnalyzers.put("host_neighbourhood", new SimpleAnalyzer()); // Para nombres de lugares
        
        PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(
            new StandardAnalyzer(), fieldAnalyzers);
        
        int totalTokens = 0;
        int muestrasAnalizadas = 0;
        
        for (String[] sample : samples) {
            if (sample.length > 40) {
                String amenities = sample[39];
                String hostNeighbourhood = sample[21];
                
                if (!amenities.isEmpty() && !hostNeighbourhood.isEmpty()) {
                    int tokensAmenities = analizarTexto(amenities, perFieldAnalyzer);
                    int tokensNeighbourhood = analizarTexto(hostNeighbourhood, perFieldAnalyzer);
                    
                    totalTokens += tokensAmenities + tokensNeighbourhood;
                    muestrasAnalizadas++;
                }
            }
        }
        
        System.out.println("Muestras analizadas: " + muestrasAnalizadas);
        System.out.println("Total de tokens: " + totalTokens);
        System.out.println("Promedio de tokens por muestra: " + (muestrasAnalizadas > 0 ? totalTokens / muestrasAnalizadas : 0));
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
     * Analiza texto con n-gramas personalizados
     */
    private static int analizarConNGramas(String texto) throws IOException {
        StandardTokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = new LowerCaseFilter(tokenizer);
        tokenStream = new StopFilter(tokenStream, EnglishAnalyzer.getDefaultStopSet());
        tokenStream = new EdgeNGramTokenFilter(tokenStream, 2, 6, false);
        
        tokenizer.setReader(new StringReader(texto));
        
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
        } else if (analyzer instanceof SpanishAnalyzer) {
            return "Tokenización + stop words en español + stemming";
        } else if (analyzer instanceof EnglishAnalyzer) {
            return "Tokenización + stop words en inglés + stemming";
        } else if (analyzer instanceof SnowballAnalyzer) {
            return "Tokenización + stemming avanzado";
        } else if (analyzer instanceof MorfologikAnalyzer) {
            return "Tokenización + lematización (forma canónica)";
        } else if (analyzer instanceof WhitespaceAnalyzer) {
            return "Solo división por espacios";
        } else if (analyzer instanceof SimpleAnalyzer) {
            return "División por espacios + minúsculas";
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
        
        // Evaluar diferentes analizadores
        evaluarAnalizadorSugerencias("StandardAnalyzer", new StandardAnalyzer(), amenitiesData, neighbourhoodData);
        evaluarAnalizadorSugerencias("EnglishAnalyzer", new EnglishAnalyzer(), amenitiesData, neighbourhoodData);
        evaluarAnalizadorSugerencias("SimpleAnalyzer", new SimpleAnalyzer(), amenitiesData, neighbourhoodData);
        evaluarAnalizadorSugerencias("SnowballAnalyzer", new SnowballAnalyzer("English"), amenitiesData, neighbourhoodData);
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
