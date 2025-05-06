import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CPFThreadExecutor_Versão3 {

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

    static class TarefaDeLeitura implements Runnable {
        private List<File> arquivos;
        private AtomicInteger validos;
        private AtomicInteger invalidos;

        public TarefaDeLeitura(List<File> arquivos, AtomicInteger validos, AtomicInteger invalidos) {
            this.arquivos = arquivos;
            this.validos = validos;
            this.invalidos = invalidos;
        }

        public void run() {
            for (File arquivo : arquivos) {
                try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
                    String linha;
                    while ((linha = br.readLine()) != null) {
                        if (validarCPF(linha)) {
                            validos.incrementAndGet();
                        } else {
                            invalidos.incrementAndGet();
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler o arquivo: " + arquivo.getName());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final int NUM_THREADS = 3;
        File pasta = new File("dados");
        File[] arquivos = pasta.listFiles((dir, name) -> name.endsWith(".txt"));

        if (arquivos == null || arquivos.length != 30) {
            System.err.println("É necessário ter exatamente 30 arquivos '.txt' na pasta 'dados'");
            return;
        }

        List<File> listaArquivos = Arrays.asList(arquivos);
        Collections.sort(listaArquivos);
        AtomicInteger validos = new AtomicInteger(0);
        AtomicInteger invalidos = new AtomicInteger(0);

        List<Thread> threadsList = new ArrayList<>();
        int porThread = listaArquivos.size() / NUM_THREADS;

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            int ini = i * porThread;
            int fim = (i == NUM_THREADS - 1) ? listaArquivos.size() : ini + porThread;
            List<File> parte = listaArquivos.subList(ini, fim);
            Thread t = new Thread(new TarefaDeLeitura(parte, validos, invalidos));
            threadsList.add(t);
            t.start();
        }

        for (Thread t : threadsList) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        long tempo = fim - inicio;

        System.out.println("Válidos: " + validos.get());
        System.out.println("Inválidos: " + invalidos.get());
        System.out.println("Tempo: " + tempo + " ms");

        String nomeArquivo = String.format("versao_3_threads.txt", NUM_THREADS);
        try (PrintWriter pw = new PrintWriter(new FileWriter(nomeArquivo))) {
            pw.println("Tempo: " + tempo + " ms");
            pw.println("Válidos: " + validos.get());
            pw.println("Inválidos: " + invalidos.get());
        }
    }
}
