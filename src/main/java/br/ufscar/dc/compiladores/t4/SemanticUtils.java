package br.ufscar.dc.compiladores.t4;

import static br.ufscar.dc.compiladores.t4.SemanticAnalyzer.functionsAndProcedures;
import static br.ufscar.dc.compiladores.t4.SemanticAnalyzer.nestedScopes;
import br.ufscar.dc.compiladores.t4.SymbolTable.TypeT4;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import org.antlr.v4.runtime.Token;

public class SemanticUtils {
    // lista de erros
    public static List<String> semanticErrors = new ArrayList<>();
    // adiciona erro à lista
    public static void addSemanticErrors(Token tok, String mensagem) {
        int linha = tok.getLine();
        //verifica erro duplicado
        if (!semanticErrors.contains("Linha " + linha + ": " + mensagem))
            semanticErrors.add(String.format("Linha %d: %s", linha, mensagem));
    }
    // checa compatibilidade entre operadores
    public static boolean verificaCompatibilidade(TypeT4 T1, TypeT4 T2) {
        boolean flag = false;

        if (T1 == TypeT4.INTEGER && T2 == TypeT4.REAL)
            flag = true;
        else if (T1 == TypeT4.REAL && T2 == TypeT4.INTEGER)
            flag = true;
        else if (T1 == TypeT4.REAL && T2 == TypeT4.REAL)
            flag = true;

        return flag;
    }
    // checa compatilibilidade entre operadores como operção lógica
    public static boolean verificaCompatibilidadeLogica(TypeT4 T1, TypeT4 T2) {
        boolean flag = false;

        if (T1 == TypeT4.INTEGER && T2 == TypeT4.REAL)
            flag = true;
        else if (T1 == TypeT4.REAL && T2 == TypeT4.INTEGER)
            flag = true;

        return flag;
    }
    // limpa o nome de um identificador
    public static String reduceName(String nome, String simbolo) {

        if (nome.contains(simbolo)) {

            boolean continua = true;
            int cont = 0;
            String nomeAux;

            while (continua) {
                nomeAux = nome.substring(cont);

                if (nomeAux.startsWith(simbolo))
                    continua = false;
                else
                    cont++;
            }

            nome = nome.substring(0, cont);
        }

        return nome;

    }
    // retorna o tipo do literal
    public static TypeT4 mapType (HashMap<String, ArrayList<String>> table, String returnType) {
        TypeT4 tipoAux;

        //remove ponteiro.
        if (returnType.charAt(0) == '^') {
            returnType = returnType.substring(1);
        }

        if (table.containsKey(returnType))
            tipoAux = TypeT4.REGISTER;
        else if (returnType.equals("literal"))
            tipoAux = TypeT4.LITERAL;
        else if (returnType.equals("inteiro"))
            tipoAux = TypeT4.INTEGER;
        else if (returnType.equals("real"))
            tipoAux = TypeT4.REAL;
        else if (returnType.equals("logico"))
            tipoAux = TypeT4.LOGIC;
        else
            tipoAux = TypeT4.INVALID;

        return tipoAux;
    }
    // métodos de verificação de tipo                
    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.Exp_aritmeticaContext ctx) {
        TypeT4 returnType = verifyType(table, ctx.termo().get(0));

        for (var termoArit : ctx.termo()) {
            TypeT4 actualType = verifyType(table, termoArit);

            if ((verificaCompatibilidade(actualType, returnType)) && (actualType != TypeT4.INVALID))
                returnType = TypeT4.REAL;
            else
                returnType = actualType;
        }

        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.TermoContext ctx) {
        TypeT4 returnType = verifyType(table, ctx.fator().get(0));

        for (LinguagemAlgoritmicaParser.FatorContext fatorArit : ctx.fator()) {
            TypeT4 actualType = verifyType(table, fatorArit);

            if ((verificaCompatibilidade(actualType, returnType)) && (actualType != TypeT4.INVALID))
                returnType = TypeT4.REAL;
            else
                returnType = actualType;
        }

        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.FatorContext ctx) {
        TypeT4 returnType = null;

        for (LinguagemAlgoritmicaParser.ParcelaContext parcela : ctx.parcela()) {
            returnType = verifyType(table, parcela);

            if (returnType == TypeT4.REGISTER) {
                String nome = parcela.getText();
                nome = reduceName(nome, "(");
                returnType = verifyType(table, nome);
            }
        }

        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null)
            return verifyType(table, ctx.parcela_unario());
        else
            return verifyType(table, ctx.parcela_nao_unario());
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.Parcela_unarioContext ctx) {
        TypeT4 returnType = null;
        String nome;

        if (ctx.identificador() != null) {
            if (!ctx.identificador().dimensao().exp_aritmetica().isEmpty())
                nome = ctx.identificador().IDENT().get(0).getText();
            else
                nome = ctx.identificador().getText();

            if (table.exists(nome)) {
                returnType = table.verify(nome);

            }
            else {
                SymbolTable tableAux = nestedScopes.getCurrentScope();

                if (!tableAux.exists(nome)) {
                    if (!ctx.identificador().getText().contains(nome)) {
                        addSemanticErrors(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                        returnType = TypeT4.INVALID;
                    } else {
                        addSemanticErrors(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                        returnType = TypeT4.INVALID;
                    }
                } else
                    returnType = tableAux.verify(nome);
            }
        } else if (ctx.IDENT() != null) {
            if (functionsAndProcedures.containsKey(ctx.IDENT().getText())) {
                List<TypeT4> aux = functionsAndProcedures.get(ctx.IDENT().getText());

                if (aux.size() == ctx.expressao().size()) {
                    for (int i = 0; i < ctx.expressao().size(); i++)
                        if (aux.get(i) != verifyType(table, ctx.expressao().get(i)))
                            addSemanticErrors(ctx.expressao().get(i).getStart(), "incompatibilidade de parametros na chamada de " + ctx.IDENT().getText());

                    returnType = aux.get(aux.size() - 1);

                } else
                    addSemanticErrors(ctx.IDENT().getSymbol(), "incompatibilidade de parametros na chamada de " + ctx.IDENT().getText());
            } else
                returnType = TypeT4.INVALID;
        } else if (ctx.NUM_INT() != null)
            returnType = TypeT4.INTEGER;
        else if (ctx.NUM_REAL() != null)
            returnType = TypeT4.REAL;
        else
            returnType = verifyType(table, ctx.expressao().get(0));

        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.Parcela_nao_unarioContext ctx) {
        TypeT4 returnType;
        String nome;

        if (ctx.identificador() != null) {
            nome = ctx.identificador().getText();

            if (!table.exists(nome)) {
                addSemanticErrors(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                returnType = TypeT4.INVALID;
            } else
                returnType = table.verify(ctx.identificador().getText());
        } else
            returnType = TypeT4.LITERAL;

        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.ExpressaoContext ctx) {
        TypeT4 returnType = verifyType(table, ctx.termo_logico(0));
        for (LinguagemAlgoritmicaParser.Termo_logicoContext termoLogico : ctx.termo_logico()) {
            TypeT4 actualType = verifyType(table, termoLogico);
            if (returnType != actualType && actualType != TypeT4.INVALID)
                returnType = TypeT4.INVALID;
        }

        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.Termo_logicoContext ctx) {
        TypeT4 returnType = verifyType(table, ctx.fator_logico(0));

        for (LinguagemAlgoritmicaParser.Fator_logicoContext fatorLogico : ctx.fator_logico()) {
            TypeT4 actualType = verifyType(table, fatorLogico);
            if (returnType != actualType && actualType != TypeT4.INVALID)
                returnType = TypeT4.INVALID;
        }

        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.Fator_logicoContext ctx) {
        TypeT4 returnType = verifyType(table, ctx.parcela_logica());
        return returnType;
    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.Parcela_logicaContext ctx) {
        TypeT4 returnType;

        if (ctx.exp_relacional() != null)
            returnType = verifyType(table, ctx.exp_relacional());
        else
            returnType = TypeT4.LOGIC;

        return returnType;

    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.Exp_relacionalContext ctx) {
        TypeT4 returnType = verifyType(table, ctx.exp_aritmetica().get(0));

        if (ctx.exp_aritmetica().size() > 1) {
            TypeT4 actualType = verifyType(table, ctx.exp_aritmetica().get(1));

            if (returnType == actualType || verificaCompatibilidadeLogica(returnType, actualType))
                returnType = TypeT4.LOGIC;
            else
                returnType = TypeT4.INVALID;
        }

        return returnType;

    }

    public static TypeT4 verifyType(SymbolTable table, LinguagemAlgoritmicaParser.IdentificadorContext ctx) {
        String varName = ctx.IDENT().get(0).getText();

        return table.verify(varName);
    }

    public static TypeT4 verifyType(SymbolTable table, String varName) {
        return table.verify(varName);
    }
}