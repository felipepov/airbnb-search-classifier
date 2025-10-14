import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardTokenizer;
// StandardFilter ya no existe en Lucene 10.x - se reemplaza por simplemente usando el tokenizer
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.es.SpanishAnalyzer; // incluye el conjunto de palabras vacías en español
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.commongrams.CommonGramsFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;


// Compilar el programa Java
// javac -cp "lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar" TokenFilterDemo.java

// Ejecutar el programa
// java -cp ".:lucene-10.3.1/modules/lucene-core-10.3.1.jar:lucene-10.3.1/modules/lucene-analysis-common-10.3.1.jar" TokenFilterDemo


public class TokenFilterDemo {
    
    public static void main(String[] args) throws IOException  {
        // Texto de ejemplo para analizar (contenido de emails.txt)
        String texto = "**De:** María López <maria.lopez92@gmail.com>\n" +
                      "**Para:** Luis Ortega <luis.ortega@outlook.com>\n" +
                      "**Asunto:** Reunión del proyecto del viernes\n\n" +
                      "Hola Luis,\n" +
                      "Estuve revisando el avance del proyecto y creo que sería bueno reunirnos el viernes para cerrar la presentación. Ya terminé los apartados de análisis y conclusiones, pero todavía tengo dudas con los gráficos y cómo justificar algunos resultados. Si te parece, podríamos vernos a media mañana, así nos da tiempo de repasar todo con calma.\n\n" +
                      "Por cierto, ¿podrías revisar si las diapositivas que subiste al Drive están actualizadas? Me pareció que faltaba la parte de resultados comparativos con el grupo A.\n\n" +
                      "Un abrazo,\n" +
                      "María";
        
        System.out.println("=== DEMOSTRACIÓN DE TOKEN FILTERS DE APACHE LUCENE ===\n");
        System.out.println("Texto original: " + texto + "\n");
        
        // 1. Tokenización básica (StandardTokenizer)
        // NOTA: StandardTokenizer es obligatorio como base - los TokenFilters no pueden procesar texto crudo,
        // solo modifican tokens ya creados por un tokenizador. StandardFilter fue deprecado en Lucene 10.x
        System.out.println("1. TOKENIZACIÓN BÁSICA (StandardTokenizer):");
        analizarTexto(texto, new StandardTokenizer());
        
        // 2. Con LowerCaseFilter: Convierte tokens a minúsculas
        System.out.println("\n2. CON LOWERCASEFILTER:");
        StandardTokenizer tokenizer2 = new StandardTokenizer();
        analizarTexto(texto, tokenizer2, new LowerCaseFilter(tokenizer2));
        
        // 3. Con StopFilter: Elimina palabras vacías (el, la, de, que, etc.)
        System.out.println("\n3. CON STOPFILTER (elimina palabras vacías):");
        // Usar el conjunto de palabras vacías en español predefinido de Lucene
        // NOTA: SpanishAnalyzer.getDefaultStopSet() proporciona un conjunto completo de palabras vacías en español
        CharArraySet stopWords = SpanishAnalyzer.getDefaultStopSet();
        StandardTokenizer tokenizer3 = new StandardTokenizer();
        analizarTexto(texto, tokenizer3, new StopFilter(tokenizer3, stopWords));
        
        // 4. Con SnowballFilter (stemming): Reduce palabras a su raíz (saltar -> salt)
        System.out.println("\n4. CON SNOWBALLFILTER (stemming - reducción a raíz):");
        StandardTokenizer tokenizer4 = new StandardTokenizer();
        analizarTexto(texto, tokenizer4, new SnowballFilter(tokenizer4, "Spanish"));
        
        // 5. Con ShingleFilter (n-gramas): Genera secuencias de n tokens consecutivos
        System.out.println("\n5. CON SHINGLEFILTER (2-gramas):");
        StandardTokenizer tokenizer5 = new StandardTokenizer();
        analizarTexto(texto, tokenizer5, new ShingleFilter(tokenizer5, 2));
	
	// Con EdgeNGramTokenFilter (prefijos): Genera subtokens con los primeros caracteres
	System.out.println("\n6. CON EdgeNGramTokenFilter (prefijos):");
        StandardTokenizer tokenizerEdge = new StandardTokenizer();
        EdgeNGramTokenFilter edgeNGramFilter = new EdgeNGramTokenFilter(tokenizerEdge, 1, 4,false);// tamano minimo 1, tamano maximo 4
        analizarTexto(texto, tokenizerEdge, edgeNGramFilter);

        // Con NGramTokenFilter (n-gramas internos): Genera todas las combinaciones posibles de subcadenas consecutivas
        System.out.println("\n7. CON NGramTokenFilter (n-gramas internos):");
        StandardTokenizer tokenizerNGram = new StandardTokenizer();
        NGramTokenFilter nGramFilter = new NGramTokenFilter(tokenizerNGram, 2, 3,false);// tamano minimo 2, tamano maximo 3
        analizarTexto(texto, tokenizerNGram, nGramFilter);

        // Con CommonGramsFilter (palabras comunes): Genera tokens compuestos("de acuerdo")
        System.out.println("\n8. CON CommonGramsFilter (palabras comunes):");
        StandardTokenizer tokenizerCommon = new StandardTokenizer();
        CommonGramsFilter commonGramsFilter = new CommonGramsFilter(tokenizerCommon, stopWords);
        analizarTexto(texto, tokenizerCommon, commonGramsFilter);

        // Con SynonymFilter: Anade sinonimos
	System.out.println("\n9. CON SynonymFilter: Anade sinonimos:");
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        builder.add(new org.apache.lucene.util.CharsRef("proyecto"), new org.apache.lucene.util.CharsRef("plan"), true);
        SynonymMap synMap = builder.build();
        StandardTokenizer tokenizerSyn = new StandardTokenizer();
        SynonymGraphFilter synFilter = new SynonymGraphFilter(tokenizerSyn, synMap, true);//estructura de grafo, mejor para frases largas
        analizarTexto(texto, tokenizerSyn, synFilter);
 

       
        System.out.println("\n=== ANÁLISIS COMPLETADO ===");
        
    }
    
    /**
     * Analiza un texto usando un TokenStream y muestra los tokens resultantes
     * @param texto - El texto a analizar
     * @param tokenizer - El tokenizador que divide el texto en tokens
     */
    private static void analizarTexto(String texto, Tokenizer tokenizer) {
        try {
            tokenizer.setReader(new StringReader(texto));
            mostrarTokens(tokenizer);
        } catch (IOException e) {
            System.err.println("Error al analizar el texto: " + e.getMessage());
        } finally {
            try {
                tokenizer.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar tokenizer: " + e.getMessage());
            }
        }
    }
    
    /**
     * Analiza un texto usando un TokenStream con filtros y muestra los tokens resultantes
     * @param texto - El texto a analizar
     * @param tokenizer - El tokenizador que divide el texto en tokens
     * @param filter - El filtro que procesa los tokens del tokenizador
     */
    private static void analizarTexto(String texto, Tokenizer tokenizer, TokenFilter filter) {
        try {
            tokenizer.setReader(new StringReader(texto));
            mostrarTokens(filter);
        } catch (IOException e) {
            System.err.println("Error al analizar el texto: " + e.getMessage());
        } finally {
            try {
                filter.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar filter: " + e.getMessage());
            }
            try {
                tokenizer.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar tokenizer: " + e.getMessage());
            }
        }
    }
    
    /**
     * Muestra los tokens de un TokenStream
     * @param tokenStream - El TokenStream que contiene los tokens a mostrar
     */
    private static void mostrarTokens(TokenStream tokenStream) throws IOException {
        // Obtener atributos del TokenStream para acceder a los datos de cada token
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posAttribute = tokenStream.addAttribute(PositionIncrementAttribute.class);
        
        // Resetear el stream al inicio para comenzar la iteración
        tokenStream.reset();
        
        int tokenCount = 0;
        // Iterar por todos los tokens del stream
        while (tokenStream.incrementToken()) {
            tokenCount++;
            // Extraer el texto del token actual
            String term = termAttribute.toString();
            // Extraer la posición del token (esto es útil para detectar filtros que eliminan tokens)
            int position = posAttribute.getPositionIncrement();
            
            // Mostrar cada token con su número, texto y posición
            System.out.printf("[%d] '%s' (pos: %d)\n", tokenCount, term, position);
        }
        
        System.out.println("Total de tokens: " + tokenCount);
        // Finalizar el procesamiento del stream
        tokenStream.end();
    }
}
