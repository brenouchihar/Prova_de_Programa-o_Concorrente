import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CPFThreadExecutor_Versão30 {

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
        private File arquivo;
        private AtomicInteger validos;
        private AtomicInteger invalidos;

        public TarefaDeLeitura(File arquivo, AtomicInteger validos, AtomicInteger invalidos) {
            this.arquivo = arquivo;
            this.validos = validos;
            this.invalidos = invalidos;
        }

        public void run() {
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

    public static void main(String[] args) throws Exception {
        final int NUM_THREADS = 30;
        File pasta = new File("dados");
        File[] arquivos = pasta.listFiles((dir, name) -> name.endsWith(".txt"));

        if (arquivos == null || arquivos.length != 30) {
            System.err.println("A pasta 'dados' deve conter exatamente 30 arquivos .txt.");
            return;
        }

        Arrays.sort(arquivos);
        AtomicInteger validos = new AtomicInteger(0);
        AtomicInteger invalidos = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>();

        long inicio = System.currentTimeMillis();

        for (File arquivo : arquivos) {
            Thread thread = new Thread(new TarefaDeLeitura(arquivo, validos, invalidos));
            threads.add(thread);
            thread.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        long tempo = fim - inicio;

        System.out.println("CPFs válidos: " + validos.get());
        System.out.println("CPFs inválidos: " + invalidos.get());
        System.out.println("Tempo total: " + tempo + " ms");

        try (PrintWriter pw = new PrintWriter(new FileWriter("versao_30_threads.txt"))) {
            pw.println("Tempo: " + tempo + " ms");
            pw.println("Válidos: " + validos.get());
            pw.println("Inválidos: " + invalidos.get());
        }
    }
}
