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

    public ThreadLeitura(int id, Socket socket,
                         BlockingQueue<String> filaEcho,
                         Runnable aoEncerrar) {
        this.id        = id;
        this.socket    = socket;
        this.filaEcho  = filaEcho;
        this.aoEncerrar = aoEncerrar;
    }

    @Override
    public void run() {
        try (Scanner in = new Scanner(socket.getInputStream())) {
            while (in.hasNextLine()) {
                String msg = in.nextLine();
                System.out.println("[Leitura #" + id + "] Recebido: " + msg);

                if (msg.equalsIgnoreCase("sair")) break;

                // Responde apenas ao remetente — não interfere nos broadcasts
                filaEcho.put("Servidor recebeu: " + msg);
            }
        } catch (Exception e) {
            System.err.println("[Leitura #" + id + "] Erro: " + e.getMessage());
        } finally {
            try {
                // Desbloqueia a ThreadEscrita para que ela encerre limpa
                filaEcho.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            aoEncerrar.run();
            System.out.println("[Leitura #" + id + "] Thread encerrada.");
        }
    }
}