package br.ufscar.dc.compiladores.t4;

import br.ufscar.dc.compiladores.t4.LinguagemAlgoritmicaParser.ProgramaContext;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class T4 {

    public static void main(String args[]) throws IOException {

        try (PrintWriter pw = new PrintWriter(new FileWriter(args[1]))) {
            try {
                CharStream cs = CharStreams.fromFileName(args[0]);
                //analisador lexico
                LinguagemAlgoritmicaLexer lexer = new LinguagemAlgoritmicaLexer(cs);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                //parser
                LinguagemAlgoritmicaParser parser = new LinguagemAlgoritmicaParser(tokens);
                ProgramaContext tree = parser.programa();
                SemanticAnalyzer t4s = new SemanticAnalyzer();
                //inicializaçao
                t4s.visitPrograma(tree);
                //verificação de erros
                SemanticUtils.semanticErrors.forEach(pw::println);
                pw.println("Fim da compilacao");
                pw.close();                
            } catch (RuntimeException e) {
            }
        }
    }
}
