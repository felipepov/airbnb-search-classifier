/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

import java.io.BufferedReader;
import java.io.FileReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;

import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.search.spell.DirectSpellChecker;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;

// Compilar el programa Java
// javac -cp "lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar:lucene-10.3.1/modules/lucene-suggest-10.3.1.jar" ComparadorSuggestion.java

// Ejecutar el programa
//  java -cp ".:lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar:lucene-10.3.1/modules/lucene-suggest-10.3.1.jar" ComparadorSuggestion

public class ComparadorSuggestion {

    public static Scanner scanner = new Scanner(System.in);

    public static List<String> tokenizeString(Analyzer analyzer, String string) {
        List<String> result = new ArrayList<String>();

        //  StandardTokenizer aux;
        String cad;
        try {
            TokenStream stream = analyzer.tokenStream(null, new StringReader(string));
            OffsetAttribute offsetAtt = stream.addAttribute(OffsetAttribute.class);
            CharTermAttribute cAtt = stream.addAttribute(CharTermAttribute.class);

            stream.reset();
            while (stream.incrementToken()) {

                //cad = stream.getAttribute(CharTermAttribute.class).toString();
                result.add(cAtt.toString() + " : (" + offsetAtt.startOffset() + "," + offsetAtt.endOffset() + ")");
            }
            stream.end();
            stream.close();
        } catch (IOException e) {
            // not thrown b/c we're using a string reader...
            throw new RuntimeException(e);
        }
        return result;
    }

    public static void displayTokens(Analyzer an, String text) throws IOException {

        List<String> tokens;

        {
            System.out.println("Analizador " + an.getClass());
            System.out.println("Origen:" + text);
            tokens = tokenizeString(an, text);
            for (String tk : tokens) {

                System.out.println("[" + tk + "] ");
            }
        }
    }

    public static List<String> readFileToTokens(String filePath, Analyzer analyzer) {
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            TokenStream tokenStream = analyzer.tokenStream("content", reader);
            ShingleFilter theFilter = new ShingleFilter(tokenStream);
            theFilter.setOutputUnigrams(false);

            CharTermAttribute charTermAttribute = theFilter.addAttribute(CharTermAttribute.class);
            theFilter.reset();

            while (theFilter.incrementToken()) {
                words.add(charTermAttribute.toString());
                // System.out.print(charTermAttribute.toString() + "##");
            }

            theFilter.end();
            theFilter.close();

        } catch (IOException e) {
            System.out.println("Error en fichero");
        }
        return words;
    }

    public static Map<String, Long> readFileToSents(String filePath) {

        Map<String, Long> sents = new HashMap<>();

        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String linea;
            // Leer línea por línea del archivo
            while ((linea = reader.readLine()) != null) {
                // Eliminar signos de puntuación y dividir la línea en palabras
                //String[] sentsLinea = linea.replaceAll("[^a-zA-Z0-9.\\s]", " ").toLowerCase().split("(\\.)");
                String[] sentsLinea = linea.toLowerCase().split("(\\.)");
// Agregar las palabras a la lista
                if (sentsLinea.length > 0) {
                    for (String sent : sentsLinea) {
                        sents.put(sent, sents.getOrDefault(sent, 0L) + 1);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error en fichero");
        }
        return sents;
    }

    public static List<String> readFileQueries(String filePath) {

        //carna  
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Leer línea por línea del archivo
            while ((line = reader.readLine()) != null) {
                // Eliminar signos de puntuación y dividir la línea en palabras

                words.add(line);

            }
        } catch (IOException e) {
            System.out.println("Error en fichero");
        }
        return words;
    }

    // Iterator que soporta pares <query,freq> 
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
                //System.out.println("--"+current.getKey());
                return new BytesRef(current.getKey());
            }
            return null;
        }

        @Override
        public boolean hasPayloads() {
            return false;
        }

        @Override
        public boolean hasContexts() {
            return false;
        }

        @Override
        public Set<BytesRef> contexts() {
            return null;
        }

        @Override
        public BytesRef payload() {
            return null;
        }
    }

    public static void NextTermSuggester(String filePath, Analyzer an_index, Analyzer an_query) throws IOException {

        System.out.println("En Next Term Suggester");
        // Create a RAM directory to store the suggester index

        Directory directory = new ByteBuffersDirectory();

        FreeTextSuggester suggester = new FreeTextSuggester(an_index, an_query, 3, (byte) 0x20);

        // Build the suggester with input queries
        Map<String, Long> sentencias = readFileToSents(filePath);
        suggester.build(new QueryFreqIterator(sentencias));

        System.out.println("Created suggester, looking for suggestion...");
        System.out.println("longitud suggester ..." + suggester.getCount());
        // Get suggestions  
        String entrada;
        do {
            // Solicitar al usuario que introduzca una cadena 
            System.out.println("Introduce un texto (FIN terminar):");

            // Leer el texto introducido por el usuarios
            entrada = scanner.nextLine();
            if (!entrada.isEmpty()) {
                List<Lookup.LookupResult> suggestions = suggester.lookup(entrada, false, 10);

                // Display suggestions
                for (Lookup.LookupResult result : suggestions) {
                    System.out.println(result.toString() + " ::" + result.key + " (" + result.value + ")");
                }
            }
        } while (!entrada.equals("FIN"));
        System.out.println("FIN");
        // Close the directory
        directory.close();
    }

    public static void InfixSuggester(String filePath, Analyzer an_index, Analyzer an_query) throws IOException {
        System.out.print("Infix Suggester .... ");
        Directory directory = new ByteBuffersDirectory();
        // Create an AnalyzingInfixSuggester  
          AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(directory, an_index, an_query,
                3, true, false, true);

        // Obtener las palabras del archivo
         
        List<String> trainingLines = readFileQueries(filePath);
        Random random = new Random();
        System.out.println("total de " + trainingLines.size() + " lineas");

        // Construir el suggester con las queries, donde creamos artificialmente las frecuencias
        Map<String, Long> queries = new HashMap<>();
        for (String word : trainingLines) {

            // Generar un número aleatorio entre 0 y 99
            int numeroAleatorio = random.nextInt(100) + 1;

            queries.put(word, queries.getOrDefault(word, 0L) + Long.valueOf(numeroAleatorio));
        }
        System.out.println("total de " + queries.size() + " frases en conjunto");

        System.out.println("Creating suggester");

        suggester.build(new QueryFreqIterator(queries));
        System.out.println("Created suggester, looking for suggestion...");
        // Get suggestions  

        String entrada;
        do {
            // Solicitar al usuario que introduzca una cadena
            System.out.println("Introduce un texto (FIN terminar):");

            // Leer el texto introducido por el usuario
            entrada = scanner.nextLine();
            System.out.println("recomendaciones  "); // Display suggestions

            List<Lookup.LookupResult> suggestions = suggester.lookup(entrada, 10, false, true);
            // Display suggestions
            for (Lookup.LookupResult result : suggestions) {
                System.out.println(result.key + " (" + result.value + ")" + result.highlightKey);
            }
        } while (!entrada.equals("FIN"));
        System.out.println("FIN");
        // Close the directory
        directory.close();

    }

    public static void PrefixSuggester(String filePath, Analyzer an_index, Analyzer an_query) throws IOException {
        System.out.print("Prefix Suggester .... ");
        Directory directory = new ByteBuffersDirectory();

        AnalyzingSuggester suggester = new AnalyzingSuggester(directory, "aaa",
                an_index, an_query, AnalyzingSuggester.PRESERVE_SEP, 256, -1, true);
        
        List<String> trainingLines = readFileQueries(filePath);
        Random random = new Random();
        System.out.println("total de " + trainingLines.size() + " lineas");

        // Construir el suggester con las queries, donde creamos artificialmente las frecuencias
        Map<String, Long> queries = new HashMap<>();
        for (String word : trainingLines) {

            // Generar un número aleatorio entre 0 y 99
            int numeroAleatorio = random.nextInt(100) + 1;

            queries.put(word, queries.getOrDefault(word, 0L) + Long.valueOf(numeroAleatorio));
        }
        System.out.println("total de " + queries.size() + " frases en conjunto");

        System.out.println("Creating suggester");

        suggester.build(new QueryFreqIterator(queries));
        System.out.println("Created suggester, looking for suggestion...");
        // Get suggestions  
        String entrada;
        do {
            // Solicitar al usuario que introduzca una cadena
            System.out.println("Introduce un texto (FIN terminar):");

            // Leer el texto introducido por el usuario
            entrada = scanner.nextLine();

            List<Lookup.LookupResult> suggestions = suggester.lookup(entrada, false, 10);

            System.out.println("recomendaciones  "); // Display suggestions
            for (Lookup.LookupResult result : suggestions) {
                System.out.println(result.key + " (" + result.value + ")" + result.highlightKey);
            }
        } while (!entrada.equals("FIN"));
        System.out.println("FIN");
        // Close the directory
        directory.close();

    }

    public static void FuzzyPrefixSuggester(String filePath, Analyzer an_index, Analyzer an_query) throws IOException {
        // Create a RAM directory to store the suggester index

        System.out.println("Fuzzy Prefix Suggester");
        Directory directory = new ByteBuffersDirectory();

        FuzzySuggester suggester = new FuzzySuggester(directory, "aaa", an_index,
                an_query);

        List<String> trainingQueries = readFileQueries(filePath);

        System.out.println("total de " + trainingQueries.size() + " palabras");
        // Build the suggester with input queries
        Map<String, Long> queries = new HashMap<>();

        for (String lines : trainingQueries) {
            queries.put(lines, queries.getOrDefault(lines, 0L) + 1);
        }
        System.out.println("total de " + trainingQueries.size() + " terminos");
//         
        System.out.println("Creating suggester");

        suggester.build(new QueryFreqIterator(queries));

        System.out.println("Created suggester, looking for suggestion...");
        // Get suggestions for the prefix "app"
        String entrada;
        do {
            // Solicitar al usuario que introduzca una cadena
            System.out.println("Introduce un texto (FIN terminar):");

            // Leer el texto introducido por el usuario
            entrada = scanner.nextLine();
            List<Lookup.LookupResult> suggestions = suggester.lookup(entrada, false, 10);

            // Display suggestions
            for (Lookup.LookupResult result : suggestions) {
                System.out.println(result.key + " (" + result.value + ")");
            }
        } while (!entrada.equals("FIN"));
        System.out.println("FIN");
        // Close the directory
        directory.close();
    }

    public static void pruebaAnalyzers() throws IOException {
        Analyzer[] ej_analyzers = new Analyzer[]{//   
            new WhitespaceAnalyzer(),
            new SimpleAnalyzer(),
            new StandardAnalyzer(),
            new EnglishAnalyzer(),
            new SpanishAnalyzer(),
            new ShingleAnalyzerWrapper(new StandardAnalyzer(), 2, 3)};

        System.out.println("prueba de analizadores size:" + ej_analyzers.length);
        String texto = "esto, es una prueba de los analizadores: enviar correo a jhg@ugr.es";
        for (Analyzer an : ej_analyzers) {

            displayTokens(an, texto);

        }

        System.out.println("Nuevo Analizador ....");
        Analyzer mi_analizador = new Analyzer() {
            @Override
            protected Analyzer.TokenStreamComponents createComponents(String fieldName) {

                Tokenizer source = new StandardTokenizer();
                TokenStream result = new LowerCaseFilter(source);

                result = new EdgeNGramTokenFilter(result, 3, 5, true);

                return new Analyzer.TokenStreamComponents(source, result);
            }
        ;
        };
      displayTokens(mi_analizador, texto);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Comparador de Analizadores con Sugerencias ....");
        
        // Seleccionar tipo de datos
        System.out.println("\n=== Seleccionar Tipo de Datos ===");
        System.out.println("1.  Amenities (amenities.csv)");
        System.out.println("2.  Host Neighbourhood (host_neighbourhood.csv)");
        System.out.print("Seleccione el tipo de datos (1 o 2): ");
        
        String dataType = scanner.nextLine().trim().toLowerCase();
        String filePath;
        
        if (dataType.equals("1") || dataType.equals("amenities")) {
            filePath = "amenities.csv";
            System.out.println("Usando datos de amenities...");
        } else if (dataType.equals("2") || dataType.equals("host neighbourhood") || dataType.equals("host neighborhood")) {
            filePath = "host_neighbourhood.csv";
            System.out.println("Usando datos de host neighbourhood...");
        } else {
            System.out.println("Opción inválida. Usando amenities por defecto.");
            filePath = "amenities.csv";
        }
        
        // Seleccionar analizador
        System.out.println("\n=== Seleccionar Analizador ===");
        System.out.println("1.  StandardAnalyzer");
        System.out.println("2.  EnglishAnalyzer");
        System.out.println("3.  Places Analyzer (StandardTokenizer + LowerCaseFilter + ASCIIFoldingFilter)");
        System.out.println("4.  Amenities Analyzer (CustomAnalyzer con ASCIIFolding)");
        System.out.print("Seleccione el analizador (1-4): ");
        
        String analyzerChoice = scanner.nextLine().trim();
        Analyzer selectedAnalyzer;
        
        try {
            switch (analyzerChoice) {
                case "1":
                    selectedAnalyzer = new StandardAnalyzer();
                    System.out.println("Usando StandardAnalyzer...");
                    break;
                case "2":
                    selectedAnalyzer = new EnglishAnalyzer();
                    System.out.println("Usando EnglishAnalyzer...");
                    break;
                case "3":
                    selectedAnalyzer = new Analyzer() {
                        @Override
                        protected Analyzer.TokenStreamComponents createComponents(String fieldName) {
                            StandardTokenizer source = new StandardTokenizer();
                            TokenStream result = new LowerCaseFilter(source);
                            result = new ASCIIFoldingFilter(result);
                            return new Analyzer.TokenStreamComponents(source, result);
                        }
                    };
                    System.out.println("Usando Places Analyzer personalizado...");
                    break;
                case "4":
                    selectedAnalyzer = CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("asciifolding")
                        .build();
                    System.out.println("Usando Amenities Analyzer personalizado...");
                    break;
                default:
                    selectedAnalyzer = new StandardAnalyzer();
                    System.out.println("Opción inválida. Usando StandardAnalyzer por defecto...");
            }
        } catch (Exception e) {
            System.out.println("Error creando analizador personalizado. Usando StandardAnalyzer...");
            selectedAnalyzer = new StandardAnalyzer();
        }
        
        int option = 0;

        // Bucle para mostrar el menú hasta que el usuario elija salir (opción 6)
        while (option != 6) {
            // Mostrar el menú
            System.out.println("\n=== Menú Principal ===");
            System.out.println("1.  Prueba de Analizadores");
            System.out.println("2.  Recomendaciones Prefijo");
            System.out.println("3.  Recomendaciones Fuzzy");
            System.out.println("4.  Recomendaciones Infijo");
            System.out.println("5.  Siguiente término");
            System.out.println("6.  Salir");
            System.out.print("Seleccione una opción: ");

            // Leer la opción desde el teclado
            option = scanner.nextInt();

            // Manejar la opción seleccionada
            switch (option) {
                case 1:
                    pruebaAnalyzers();
                    break;
                case 2:
                    PrefixSuggester(filePath, selectedAnalyzer, selectedAnalyzer);
                    break;
                case 3:
                    FuzzyPrefixSuggester(filePath, selectedAnalyzer, selectedAnalyzer);
                    break;
                case 4:
                    InfixSuggester(filePath, selectedAnalyzer, selectedAnalyzer);
                    break;
                case 5:
                    NextTermSuggester(filePath, selectedAnalyzer, selectedAnalyzer);
                    break;
                case 6:
                    System.out.println("Saliendo del programa...");
                    break;
                default:
                    System.out.println("Opción inválida, por favor seleccione una opción del 1 al 6.");
            }

        }
        // Create a RAM directory to store the suggester index
        scanner.close();
    }
}
