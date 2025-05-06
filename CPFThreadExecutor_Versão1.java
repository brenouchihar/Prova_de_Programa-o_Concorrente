import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CPFThreadExecutor_Versão1 {

    // === Validador de CPF ===
    public static boolean validarCPF(String cpf) {
        cpf = cpf.replaceAll("\\D", "");
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) return false;

        for (int i = 9; i <= 10; i++) {
            int soma = 0;
            for (int j = 0; j < i; j++) {
                soma += (cpf.charAt(j) - '0') * ((i + 1) - j);
            }
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != (cpf.charAt(i) - '0')) return false;
        }
        return true;
    }

    // === Tarefa que processa arquivos (executada por thread) ===
    static class ProcessoDeArquivos implements Runnable {
        private List<File> arquivos;
        private AtomicInteger validos;
        private AtomicInteger invalidos;

        public ProcessoDeArquivos(List<File> arquivos, AtomicInteger validos, AtomicInteger invalidos) {
            this.arquivos = arquivos;
            this.validos = validos;
            this.invalidos = invalidos;
        }

        @Override
        public void run() {
            for (File arquivo : arquivos) {
                try (BufferedReader leitor = new BufferedReader(new FileReader(arquivo))) {
                    String linha;
                    while ((linha = leitor.readLine()) != null) {
                        if (validarCPF(linha)) {
                            validos.incrementAndGet();
                        } else {
                            invalidos.incrementAndGet();
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler: " + arquivo.getName());
                }
            }
        }
    }

    // === Função principal ===
    public static void main(String[] args) throws Exception {
        final int NUM_THREADS = 1; // <<< ALTERE ESTE VALOR PARA 2, 3, 5, 6, 10, 15, 30

        File pasta = new File("dados");
        File[] todosArquivos = pasta.listFiles((dir, name) -> name.endsWith(".txt"));

        if (todosArquivos == null || todosArquivos.length != 30) {
            System.err.println("É necessário ter exatamente 30 arquivos '.txt' na pasta 'dados'");
            return;
        }

        List<File> listaArquivos = Arrays.asList(todosArquivos);
        Collections.sort(listaArquivos); // garante ordem consistente
        AtomicInteger validos = new AtomicInteger(0);
        AtomicInteger invalidos = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>();
        int arquivosPorThread = listaArquivos.size() / NUM_THREADS;

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            int inicioIndice = i * arquivosPorThread;
            int fimIndice = (i == NUM_THREADS - 1) ? listaArquivos.size() : inicioIndice + arquivosPorThread;
            List<File> subLista = listaArquivos.subList(inicioIndice, fimIndice);

            Thread t = new Thread(new ProcessoDeArquivos(subLista, validos, invalidos));
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        long duracao = fim - inicio;

        System.out.println("CPFs válidos: " + validos.get());
        System.out.println("CPFs inválidos: " + invalidos.get());
        System.out.println("Tempo total: " + duracao + " ms");

        // === Salvar resultado em arquivo ===
        String nomeArquivo = String.format("versao_%d_thread%s.txt", NUM_THREADS,
                NUM_THREADS == 1 ? "" : "s");
        try (PrintWriter out = new PrintWriter(new FileWriter(nomeArquivo))) {
            out.println("Tempo de execução: " + duracao + " ms");
            out.println("CPFs válidos: " + validos.get());
            out.println("CPFs inválidos: " + invalidos.get());
        }
    }
}
