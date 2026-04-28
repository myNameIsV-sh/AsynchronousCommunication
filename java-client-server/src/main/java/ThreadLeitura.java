import java.util.Scanner;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ThreadLeitura implements Runnable {

    // Sentinela que sinaliza à ThreadEscrita para encerrar
    public static final String POISON_PILL = "__ENCERRAR__";

    private final int id;
    private final Socket socket;
    private final BlockingQueue<String> filaEcho;      // respostas ao próprio cliente
    private final Runnable aoEncerrar;

    private final Servidor servidor; // referência para disparar o broadcast

    public ThreadLeitura(int id, Socket socket,
                         BlockingQueue<String> filaEcho,
                         Runnable aoEncerrar,
                         Servidor servidor) {
        this.id        = id;
        this.socket    = socket;
        this.filaEcho  = filaEcho;
        this.aoEncerrar = aoEncerrar;
        this.servidor  = servidor;
    }

    @Override
    public void run() {
        try (java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(socket.getInputStream()))) {
            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println("[Leitura #" + id + "] Recebido de Cliente #" + id + ": " + msg);

                if (msg.equalsIgnoreCase("sair")) break;

                filaEcho.put("Servidor recebeu: " + msg);
                servidor.realizarBroadcast("[Cliente #" + id + "]: " + msg, id);
            }
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("[Leitura #" + id + "] Cliente #" + id + " removido por inatividade.");
            try {
                filaEcho.put("Você foi desconectado por inatividade.");
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            System.err.println("[Leitura #" + id + "] Erro: " + e.getMessage());
        } finally {
            try {
                filaEcho.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            aoEncerrar.run();
            System.out.println("[Leitura #" + id + "] Thread encerrada.");
        }
    }
}