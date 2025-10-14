import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

public class AnalizadorPropio {

    public static void main(String[] args) throws IOException {
        // Dos tweets de ejemplo
        String tweet1 = "@Emilio_GA Me encanta aprender sobre informática y cómo la tecnología puede mejorar el mundo :-) ;) #Innovacion #Software";
        String tweet2 = "@Felipillo Hoy visité el museo de arte contemporáneo, una experiencia horrible :-( #Cultura #Arte";

	System.out.println("Texto original:   \n" + tweet1 + "\n" + tweet2 + "\n");

        System.out.println("=== Analizador personalizado para Red Social X ===\n");

        System.out.println("Tweet 1:");
        analizarTexto(tweet1);

        System.out.println("\nTweet 2:");
        analizarTexto(tweet2);

        System.out.println("\n=== Análisis completado ===");
    }

    private static void analizarTexto(String texto) throws IOException {
        // Patrón que reconoce menciones(@\\w+), hashtags(#\\w+), emojis(:\\)|:-\\)|:\\(|:-\\) y palabras (\\p{L}+)
        Pattern patron = Pattern.compile("@\\w+|#\\w+|:\\)|:-\\)|:\\(|:-\\(|;-\\)|;\\)|\\p{L}+");

        // Tokenizador
        PatternTokenizer tokenizer = new PatternTokenizer(patron, 0);

        // NO vamos a poner filtro de minúsculas porque consideramos que es importante diferenciar mayusculas y minusculas
	// para el nombre de los usuarios (@Juan123 no es @JUAN123)
	TokenStream tokenStream = tokenizer;

        // Filtro de palabras vacías
        CharArraySet stopWords = SpanishAnalyzer.getDefaultStopSet();
        tokenStream = new StopFilter(tokenStream, stopWords);

        // Crear un mapa de sinónimos para emojis
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        builder.add(new org.apache.lucene.util.CharsRef(":-)"), new org.apache.lucene.util.CharsRef("feliz"), true);
        builder.add(new org.apache.lucene.util.CharsRef(":)"), new org.apache.lucene.util.CharsRef("feliz"), true);
        builder.add(new org.apache.lucene.util.CharsRef(":-("), new org.apache.lucene.util.CharsRef("triste"), true);
        builder.add(new org.apache.lucene.util.CharsRef(":("), new org.apache.lucene.util.CharsRef("triste"), true);
	builder.add(new org.apache.lucene.util.CharsRef(";-)"), new org.apache.lucene.util.CharsRef("guiño"), true);
	builder.add(new org.apache.lucene.util.CharsRef(";)"), new org.apache.lucene.util.CharsRef("guiño"), true);
	

        SynonymMap map = builder.build();

        // Aplicar el filtro de sinónimos
        tokenStream = new SynonymGraphFilter(tokenStream, map, true);

        // Analizar
        tokenizer.setReader(new StringReader(texto));
        mostrarTokens(tokenStream);
    }

    private static void mostrarTokens(TokenStream tokenStream) throws IOException {
        CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        int count = 0;
        while (tokenStream.incrementToken()) {
            count++;
            System.out.printf("[%d] %s\n", count, termAttr.toString());
        }
        tokenStream.end();
        tokenStream.close();
        System.out.println("Total de tokens: " + count);
    }
}

